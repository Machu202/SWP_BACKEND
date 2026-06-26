package com.mangastudio.backend.service.impl;

import com.mangastudio.backend.dto.response.BoardVoteSummaryResponse;
import com.mangastudio.backend.entity.BoardVote;
import com.mangastudio.backend.entity.MangaSeries;
import com.mangastudio.backend.entity.User;
import com.mangastudio.backend.repository.BoardVoteRepository;
import com.mangastudio.backend.repository.MangaSeriesRepository;
import com.mangastudio.backend.repository.UserRepository;
import com.mangastudio.backend.service.BoardVoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BoardVoteServiceImpl implements BoardVoteService {

    private final BoardVoteRepository boardVoteRepository;
    private final MangaSeriesRepository mangaSeriesRepository;
    private final UserRepository userRepository;

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
        return boardVoteRepository.save(vote);
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
}