package com.kfarms.repository;

import com.kfarms.entity.SupportTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {

    @Query("""
        select distinct t
        from SupportTicket t
        join fetch t.tenant tenant
        left join fetch t.messages m
        where (t.deleted = false or t.deleted is null)
        order by t.updatedAt desc, t.id desc
    """)
    List<SupportTicket> findAllActiveWithTenantAndMessages();

    @Query("""
        select distinct t
        from SupportTicket t
        left join fetch t.messages m
        where t.tenant.id = :tenantId
          and (t.deleted = false or t.deleted is null)
        order by t.updatedAt desc, t.id desc
    """)
    List<SupportTicket> findAllActiveByTenantIdWithMessages(@Param("tenantId") Long tenantId);

    @Query("""
        select distinct t
        from SupportTicket t
        left join fetch t.messages m
        where t.ticketCode = :ticketCode
          and t.tenant.id = :tenantId
          and (t.deleted = false or t.deleted is null)
    """)
    Optional<SupportTicket> findByTicketCodeAndTenantIdWithMessages(
            @Param("ticketCode") String ticketCode,
            @Param("tenantId") Long tenantId
    );

    @Query("""
        select distinct t
        from SupportTicket t
        left join fetch t.messages m
        where t.id = :id
          and t.tenant.id = :tenantId
          and (t.deleted = false or t.deleted is null)
    """)
    Optional<SupportTicket> findByIdAndTenantIdWithMessages(
            @Param("id") Long id,
            @Param("tenantId") Long tenantId
    );
}
