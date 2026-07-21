package com.mangastudio.backend.listener;

import org.springframework.stereotype.Component;

@Component
public class PageVersioningListener {
    // The JPA listener is disabled. Draft versioning (FE-43)
}

/*package com.mangastudio.backend.listener;

import com.mangastudio.backend.entity.Page;
import com.mangastudio.backend.entity.PageVersion;
import com.mangastudio.backend.repository.PageVersionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostUpdate;
import java.time.LocalDateTime;

@Component
public class PageVersioningListener {

    @Autowired
    @Lazy
    private PageVersionRepository pageVersionRepository;

    @Autowired
    @Lazy
    private EntityManager entityManager;

    // Do not use REQUIRES_NEW.
    // REQUIRED keeps PageVersion in the same transaction as PageServiceImpl.
    @Transactional(propagation = Propagation.REQUIRED)
    @PostPersist
    @PostUpdate
    public void savePageSnapshot(Page page) {
        // Safety guard: stop when the page does not have a valid ID.
        if (page.getId() == null) {
            return;
        }

        PageVersion newVersion = new PageVersion();
        
        // getReference() creates a Hibernate proxy for the row with page.getId()
        // in the persistence context, avoiding a transient-instance foreign-key error.
        Page managedPageProxy = entityManager.getReference(Page.class, page.getId());
        newVersion.setPage(managedPageProxy);
        
        newVersion.setImageUrl(page.getImageUrl());
        newVersion.setCreatedAt(LocalDateTime.now());

        try {
            int currentVersionsCount = pageVersionRepository.countByPageId(page.getId());
            newVersion.setVersionNumber(currentVersionsCount + 1);
        } catch (Exception e) {
            newVersion.setVersionNumber(1);
        }

        pageVersionRepository.save(newVersion);
        
        System.out.println(">>> [VERSIONING LISTENER] Successfully backed up snapshot Version " 
                + newVersion.getVersionNumber() + " for Page ID: " + page.getId());
    }
}*/
