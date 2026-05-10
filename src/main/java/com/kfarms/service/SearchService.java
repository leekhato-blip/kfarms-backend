package com.kfarms.service;

import com.kfarms.dto.SearchResultDto;
import com.kfarms.health.repo.HealthEventRepo;
import com.kfarms.repository.*;
import com.kfarms.tenant.service.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final FishPondRepository fishPondRepo;
    private final FishHatchRepository fishHatchRepo;
    private final FeedRepository feedRepo;
    private final SuppliesRepository suppliesRepo;
    private final SalesRepository salesRepo;
    private final LivestockRepository livestockRepo;
    private final EggProductionRepo eggProductionRepo;
    private final InventoryRepository inventoryRepo;
    private final HealthEventRepo healthRepo;
    private final TaskRepository taskRepo;
    private final NotificationRepository notificationRepo;


    public List<SearchResultDto> search(String q, int limit) {
        Long tenantId = TenantContext.getTenantId();
        String query = (q == null) ? "" : q.trim();
        if (query.isBlank()) return List.of();

        int safeLimit = Math.max(1, Math.min(limit, 50));
        int perEntity = Math.max(2, safeLimit / 5);

        List<Scored> all = new ArrayList<>();

        // 1) Static pages
        addPageMatches(all, query);

        // 2) Entity matches
        if (tenantId != null) {
            fishPondRepo.searchByName(tenantId, query, PageRequest.of(0, perEntity))
                    .forEach(p -> addIfMatch(all, query,
                            new SearchResultDto(p.getId(), p.getPondName(), "Fish Pond", "/fish-ponds"),
                            p.getPondName()
                    ));

            feedRepo.searchByName(tenantId, query, PageRequest.of(0, perEntity))
                    .forEach(f -> addIfMatch(all, query,
                            new SearchResultDto(f.getId(), f.getFeedName(), "Feed", "/feeds"),
                            safeJoin(f.getFeedName())
                    ));

            suppliesRepo.searchByItemName(tenantId, query, PageRequest.of(0, perEntity))
                    .forEach(s -> addIfMatch(all, query,
                            new SearchResultDto(s.getId(), s.getItemName(), "Supply", "/supplies"),
                            s.getItemName()
                    ));

            fishHatchRepo.searchByName(tenantId, query, PageRequest.of(0, perEntity))
                    .forEach(h -> {
                        String pondName = (h.getPond() != null) ? h.getPond().getPondName() : null;

                        String title = (pondName != null && !pondName.isBlank())
                                ? ("Hatch · " + pondName)
                                : "Fish Hatch";

                        String subtitle = (h.getHatchDate() != null)
                                ? ("Date: " + h.getHatchDate())
                                : "Fish Hatch";

                        String haystack = safeJoin(
                                pondName,
                                h.getNote(),
                                String.valueOf(h.getHatchDate())
                        );

                        addIfMatch(all, query,
                                new SearchResultDto(
                                        h.getId(),
                                        title,
                                        subtitle,
                                        "/fish-ponds"
                                ),
                                haystack
                        );
                    });

            salesRepo.searchByItemName(tenantId, query, PageRequest.of(0, perEntity))
                    .forEach(sale -> addIfMatch(all, query,
                            new SearchResultDto(sale.getId(), sale.getItemName(), "Sale", "/sales"),
                            safeJoin(sale.getItemName(), String.valueOf(sale.getQuantity()))
                    ));

            livestockRepo.searchByNameOrBatch(tenantId, query, PageRequest.of(0, perEntity))
                    .forEach(l -> {
                        String batch = l.getBatchName();
                        String title = (batch != null && !batch.isBlank()) ? batch : "Poultry";
                        String subtitle = "Poultry";
                        String haystack = safeJoin(batch);

                        addIfMatch(all, query,
                                new SearchResultDto(l.getId(), title, subtitle, "/poultry"),
                                haystack
                        );
                    });

            eggProductionRepo.searchByLivestockOrNote(tenantId, query, PageRequest.of(0, perEntity))
                    .forEach(e -> {
                        String batchName = (e.getLivestock() != null) ? e.getLivestock().getBatchName() : null;
                        String livestockName = (batchName != null && !batchName.isBlank()) ? batchName : "Poultry";

                        String subtitle = (batchName != null && !batchName.isBlank())
                                ? ("Batch: " + batchName)
                                : "Egg Production";

                        String title = livestockName + " · " + e.getCollectionDate();

                        String haystack = safeJoin(livestockName, batchName, e.getNote(), String.valueOf(e.getCollectionDate()));

                        addIfMatch(all, query,
                                new SearchResultDto(e.getId(), title, subtitle, "/productions"),
                                haystack
                        );
                    });

            inventoryRepo.searchByItemName(tenantId, query, PageRequest.of(0, perEntity))
                    .forEach(i -> addIfMatch(all, query,
                            new SearchResultDto(i.getId(), i.getItemName(), "Inventory", "/inventory"),
                            i.getItemName()
                    ));

            healthRepo.search(tenantId, query, PageRequest.of(0, perEntity))
                .forEach(e -> addIfMatch(all, query,
                        new SearchResultDto(
                                e.getId(),
                                e.getRule().getTitle(),
                                "Health Event · " + e.getSeverity(),
                                "/dashboard"
                        ),
                        safeJoin(e.getRule().getTitle(), e.getSourceKey(), e.getContextNote())
                ));
        }

//        notificationRepo.searchByTitleOrMessage(query, PageRequest.of(0, perEntity))
//                .forEach(n -> {
//                    String nTitle = (n.getTitle() != null && !n.getTitle().isBlank())
//                            ? n.getTitle()
//                            : (n.getMessage() != null && n.getMessage().length() > 40
//                            ? n.getMessage().substring(0, 40) + "…"
//                            : (n.getMessage() == null ? "Notification" : n.getMessage()));
//
//                    String haystack = safeJoin(n.getTitle(), n.getMessage());
//
//                    addIfMatch(all, query,
//                            new SearchResultDto(n.getId(), nTitle, "Notification", "/notifications/" + n.getId()),
//                            haystack
//                    );
//                });


        // Sort, dedupe by result identity, limit
        all.sort(Comparator.comparingInt(Scored::score).reversed()
                .thenComparing(x -> x.dto().getTitle(), String.CASE_INSENSITIVE_ORDER));

        Set<String> seenResults = new HashSet<>();
        List<SearchResultDto> out = new ArrayList<>();
        for (Scored s : all) {
            SearchResultDto dto = s.dto();
            if (seenResults.add(buildResultKey(dto))) {
                out.add(dto);
                if (out.size() >= safeLimit) break;
            }
        }
        return out;
    }

    /* -------------------------- Static Pages -------------------------- */

    private void addPageMatches(List<Scored> all, String q) {
        addPage(all, q, "Dashboard", null, "/dashboard");
        addPage(all, q, "Fish Ponds", "Page", "/fish-ponds");
        addPage(all, q, "Fish Hatches", "Page", "/fish-ponds");
        addPage(all, q, "Feeds", "Page", "/feeds");
        addPage(all, q, "Supplies", "Page", "/supplies");
        addPage(all, q, "Sales", "Page", "/sales");
        addPage(all, q, "Poultry", "Page", "/poultry");
        addPage(all, q, "Productions", "Page", "/productions");
        addPage(all, q, "Inventory", "Page", "/inventory");
        addPage(all, q, "Notifications", "Page", "/dashboard");
    }

    private void addPage(List<Scored> all, String q, String title, String subtitle, String url) {
        int s = simpleScore(q, title);
        if (s > 0) {
            all.add(new Scored(s, new SearchResultDto("pages:" + url, title, subtitle, url)));
        }
    }

    /* -------------------------- Scoring Helpers -------------------------- */

    private void addIfMatch(List<Scored> all, String q, SearchResultDto dto, String haystack) {
        int s = simpleScore(q, haystack);
        if (s > 0) all.add(new Scored(s, dto));
    }

    // startsWith > contains > exact match highest
    private int simpleScore(String q, String text) {
        if (text == null) return 0;
        String qq = q.toLowerCase();
        String tt = text.toLowerCase();

        if (tt.equals(qq)) return 100;   // ✅ fixed
        if (tt.startsWith(qq)) return 70;
        if (tt.contains(qq)) return 40;
        return 0;
    }

    private String safeJoin(Object... parts) {
        StringBuilder sb = new StringBuilder();
        for (Object p : parts) {
            if (p == null) continue;
            String s = String.valueOf(p).trim();
            if (s.isBlank()) continue;
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(s);
        }
        return sb.toString();
    }

    private String buildResultKey(SearchResultDto dto) {
        return safeJoin(dto.getUrl(), dto.getId(), dto.getTitle(), dto.getSubtitle());
    }

    private record Scored(int score, SearchResultDto dto) {}
}
