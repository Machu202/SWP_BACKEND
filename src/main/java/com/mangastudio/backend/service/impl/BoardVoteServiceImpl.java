package com.mangastudio.backend.service.impl;

import com.mangastudio.backend.dto.response.BoardVoteHistoryResponse;
import com.mangastudio.backend.dto.response.AdminBoardVoteHistoryResponse;
import com.mangastudio.backend.dto.response.BoardVoteSummaryResponse;
import com.mangastudio.backend.entity.BoardVote;
import com.mangastudio.backend.entity.BoardVoteHistory;
import com.mangastudio.backend.entity.MangaSeries;
import com.mangastudio.backend.entity.User;
import com.mangastudio.backend.repository.BoardVoteHistoryRepository;
import com.mangastudio.backend.repository.BoardVoteRepository;
import com.mangastudio.backend.repository.MangaSeriesRepository;
import com.mangastudio.backend.repository.UserRepository;
import com.mangastudio.backend.service.BoardVoteService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class BoardVoteServiceImpl implements BoardVoteService {

    private final BoardVoteRepository boardVoteRepository;
    private final BoardVoteHistoryRepository boardVoteHistoryRepository;
    private final MangaSeriesRepository mangaSeriesRepository;
    private final UserRepository userRepository;

    public BoardVoteServiceImpl(BoardVoteRepository boardVoteRepository,
                                BoardVoteHistoryRepository boardVoteHistoryRepository,
                                MangaSeriesRepository mangaSeriesRepository,
                                UserRepository userRepository) {
        this.boardVoteRepository = boardVoteRepository;
        this.boardVoteHistoryRepository = boardVoteHistoryRepository;
        this.mangaSeriesRepository = mangaSeriesRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public BoardVote castVote(Long seriesId, Long memberId, boolean isApproved) {
        MangaSeries series = mangaSeriesRepository.findById(seriesId)
                .orElseThrow(() -> new RuntimeException("Error: Manga Series not found"));

        // Khóa logic 1: Chỉ dự án đang chờ duyệt mới được phép bỏ phiếu
        if (!"REVIEWING".equalsIgnoreCase(series.getStatus())) {
            throw new RuntimeException("Error: Can only vote on projects that are in 'REVIEWING' status.");
        }

        User member = userRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Error: Board Member not found"));

        // Khóa logic 2: Kiểm tra xem thành viên này đã bỏ phiếu chưa.
        // Nếu đã bỏ rồi thì cập nhật lại quyết định (Cho phép đổi ý), nếu chưa thì tạo phiếu mới.
        BoardVote vote = boardVoteRepository.findByMangaSeriesIdAndBoardMemberId(seriesId, memberId)
                .orElse(BoardVote.builder()
                        .mangaSeries(series)
                        .boardMember(member)
                        .build());

        vote.setIsApproved(isApproved);
        BoardVote savedVote = boardVoteRepository.save(vote);
        boardVoteHistoryRepository.save(new BoardVoteHistory(
                null,
                series,
                member,
                isApproved,
                LocalDateTime.now()));
        return savedVote;
    }

    @Override
    public BoardVoteSummaryResponse getVoteSummary(Long seriesId) {
        // Kiểm tra dự án tồn tại
        mangaSeriesRepository.findById(seriesId)
                .orElseThrow(() -> new RuntimeException("Error: Manga Series not found"));

        // Thực hiện lệnh đếm SQL siêu tốc
        long total = boardVoteRepository.countByMangaSeriesId(seriesId);
        long approved = boardVoteRepository.countByMangaSeriesIdAndIsApproved(seriesId, true);
        long rejected = boardVoteRepository.countByMangaSeriesIdAndIsApproved(seriesId, false);

        return BoardVoteSummaryResponse.builder()
                .seriesId(seriesId)
                .totalVotes(total)
                .approvedVotes(approved)
                .rejectedVotes(rejected)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BoardVoteHistoryResponse> getMyVoteHistory(Long memberId) {
        User member = userRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Error: Board Member not found"));
        String roleName = member.getRole() != null ? member.getRole().getRoleName() : "";
        if (!"Editorial Board".equalsIgnoreCase(roleName) && !"Admin".equalsIgnoreCase(roleName)) {
            throw new RuntimeException("Error: Only Editorial Board members can view vote history.");
        }

        List<BoardVoteHistoryResponse> result = new ArrayList<>();
        Set<Long> seriesWithAuditHistory = new HashSet<>();
        for (BoardVoteHistory history : boardVoteHistoryRepository.findHistoryForMember(memberId)) {
            MangaSeries series = history.getMangaSeries();
            if (series != null && series.getId() != null) seriesWithAuditHistory.add(series.getId());
            result.add(toHistoryResponse(history.getId(), series, history.getIsApproved(), history.getVotedAt()));
        }

        // Votes created before the audit table was introduced remain visible.
        for (BoardVote currentVote : boardVoteRepository.findCurrentVotesForMember(memberId)) {
            MangaSeries series = currentVote.getMangaSeries();
            if (series != null && seriesWithAuditHistory.contains(series.getId())) continue;
            result.add(toHistoryResponse(currentVote.getId(), series, currentVote.getIsApproved(), currentVote.getCreatedAt()));
        }

        result.sort(Comparator.comparing(
                BoardVoteHistoryResponse::getVotedAt,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminBoardVoteHistoryResponse> getAdminVoteHistory() {
        List<AdminBoardVoteHistoryResponse> result = new ArrayList<>();
        Set<String> auditedMemberSeries = new HashSet<>();

        for (BoardVoteHistory history : boardVoteHistoryRepository.findAllHistoryWithDetails()) {
            User member = history.getBoardMember();
            MangaSeries series = history.getMangaSeries();
            auditedMemberSeries.add(memberSeriesKey(member, series));
            result.add(toAdminHistoryResponse(
                    history.getId(), member, series, history.getIsApproved(), history.getVotedAt()));
        }

        // Preserve visibility for votes created before the immutable audit table existed.
        for (BoardVote vote : boardVoteRepository.findAllCurrentVotesWithDetails()) {
            if (auditedMemberSeries.contains(memberSeriesKey(vote.getBoardMember(), vote.getMangaSeries()))) continue;
            result.add(toAdminHistoryResponse(
                    vote.getId(), vote.getBoardMember(), vote.getMangaSeries(),
                    vote.getIsApproved(), vote.getCreatedAt()));
        }

        result.sort(Comparator.comparing(
                AdminBoardVoteHistoryResponse::getVotedAt,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return result;
    }

    private String memberSeriesKey(User member, MangaSeries series) {
        return String.valueOf(member != null ? member.getId() : null)
                + ":" + String.valueOf(series != null ? series.getId() : null);
    }

    private AdminBoardVoteHistoryResponse toAdminHistoryResponse(Long voteId, User member,
                                                                 MangaSeries series, Boolean approved,
                                                                 LocalDateTime votedAt) {
        return new AdminBoardVoteHistoryResponse(
                voteId,
                member != null ? member.getId() : null,
                displayName(member),
                series != null ? series.getId() : null,
                series != null && series.getTitle() != null ? series.getTitle() : "Manga Series",
                series != null ? series.getCoverImageUrl() : null,
                approved,
                votedAt);
    }

    private String displayName(User user) {
        if (user == null) return "Editorial Board member";
        if (user.getFullName() != null && !user.getFullName().isBlank()) return user.getFullName().trim();
        if (user.getUsername() != null && !user.getUsername().isBlank()) return user.getUsername().trim();
        return "Editorial Board #" + user.getId();
    }

    private BoardVoteHistoryResponse toHistoryResponse(Long voteId, MangaSeries series,
                                                       Boolean approved, LocalDateTime votedAt) {
        return new BoardVoteHistoryResponse(
                voteId,
                series != null ? series.getId() : null,
                series != null ? series.getTitle() : "Manga Series",
                series != null ? series.getCoverImageUrl() : null,
                series != null ? series.getGenre() : null,
                series != null ? series.getSummary() : null,
                series != null ? series.getDescription() : null,
                series != null ? series.getStatus() : null,
                approved,
                votedAt);
    }
}
