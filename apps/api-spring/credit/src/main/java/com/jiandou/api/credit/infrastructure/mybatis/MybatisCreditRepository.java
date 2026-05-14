package com.jiandou.api.credit.infrastructure.mybatis;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jiandou.api.auth.domain.UserStatus;
import com.jiandou.api.auth.infrastructure.mybatis.SysUserEntity;
import com.jiandou.api.auth.infrastructure.mybatis.SysUserMapper;
import com.jiandou.api.credit.domain.CreditFeatureCode;
import com.jiandou.api.credit.domain.CreditTransactionContext;
import com.jiandou.api.credit.dto.AdminCreditRuleResponse;
import com.jiandou.api.credit.dto.AdminCreditTransactionResponse;
import com.jiandou.api.credit.dto.AdminCreditUserResponse;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.stereotype.Repository;

/**
 * 积分持久化仓储。
 */
@Repository
public class MybatisCreditRepository {

    public static final int DEFAULT_INITIAL_BALANCE = 50;

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final SqlSessionFactory sqlSessionFactory;
    private final ObjectMapper objectMapper;

    public MybatisCreditRepository(SqlSessionFactory sqlSessionFactory, ObjectMapper objectMapper) {
        this.sqlSessionFactory = sqlSessionFactory;
        this.objectMapper = objectMapper;
    }

    public boolean isAdminUsername(Long userId) {
        if (userId == null) {
            return false;
        }
        try (SqlSession session = sqlSessionFactory.openSession()) {
            SysUserEntity user = session.getMapper(SysUserMapper.class).selectById(userId);
            return user != null && isAdminUsername(user.getUsername());
        }
    }

    public Long adminUserId() {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            SysUserEntity user = session.getMapper(SysUserMapper.class).selectOne(
                Wrappers.<SysUserEntity>lambdaQuery()
                    .eq(SysUserEntity::getUsername, "admin")
                    .last("LIMIT 1")
            );
            return user == null ? null : user.getId();
        }
    }

    public void ensureAccount(Long userId) {
        ensureAccount(userId, DEFAULT_INITIAL_BALANCE);
    }

    public void ensureAccount(Long userId, int initialBalance) {
        if (userId == null || isAdminUsername(userId)) {
            return;
        }
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            ensureAccount(session, userId, Math.max(0, initialBalance));
        }
    }

    public int ruleCost(String featureCode) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            SysCreditRuleEntity rule = findRule(session, featureCode);
            return rule == null || rule.getCost() == null ? 0 : Math.max(0, rule.getCost());
        }
    }

    public List<AdminCreditRuleResponse> listRules() {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            return session.getMapper(SysCreditRuleMapper.class).selectList(
                Wrappers.<SysCreditRuleEntity>lambdaQuery().orderByAsc(SysCreditRuleEntity::getFeatureCode)
            ).stream().map(this::toRuleResponse).toList();
        }
    }

    public AdminCreditRuleResponse updateRule(String featureCode, int cost) {
        String normalizedFeatureCode = CreditFeatureCode.normalize(featureCode);
        if (normalizedFeatureCode.isBlank()) {
            throw new IllegalArgumentException("功能编码不能为空");
        }
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            SysCreditRuleMapper mapper = session.getMapper(SysCreditRuleMapper.class);
            SysCreditRuleEntity existing = findRule(session, normalizedFeatureCode);
            if (existing == null) {
                SysCreditRuleEntity entity = new SysCreditRuleEntity();
                entity.setFeatureCode(normalizedFeatureCode);
                entity.setDisplayName(defaultRuleName(normalizedFeatureCode));
                entity.setCost(Math.max(0, cost));
                mapper.insert(entity);
            } else {
                SysCreditRuleEntity update = new SysCreditRuleEntity();
                update.setCost(Math.max(0, cost));
                mapper.update(update, Wrappers.<SysCreditRuleEntity>lambdaUpdate()
                    .eq(SysCreditRuleEntity::getFeatureCode, normalizedFeatureCode));
            }
        }
        try (SqlSession session = sqlSessionFactory.openSession()) {
            return toRuleResponse(findRule(session, normalizedFeatureCode));
        }
    }

    public CreditBalance accountBalance(Long userId) {
        try (SqlSession session = sqlSessionFactory.openSession(false)) {
            SysCreditAccountEntity account = account(session, userId, false);
            return account == null
                ? new CreditBalance(0, 0, 0)
                : new CreditBalance(defaultInt(account.getBalance()), defaultInt(account.getTotalConsumed()), defaultInt(account.getTotalAdjusted()));
        }
    }

    public ChargeResult consume(Long userId, String featureCode, int cost, CreditTransactionContext context) {
        String normalizedFeatureCode = CreditFeatureCode.normalize(featureCode);
        int normalizedCost = Math.max(0, cost);
        try (SqlSession session = sqlSessionFactory.openSession(false)) {
            ensureAccount(session, userId, DEFAULT_INITIAL_BALANCE);
            SysCreditAccountMapper accountMapper = session.getMapper(SysCreditAccountMapper.class);
            int updated = accountMapper.update(null, Wrappers.<SysCreditAccountEntity>lambdaUpdate()
                .setSql("balance = balance - " + normalizedCost)
                .setSql("total_consumed = total_consumed + " + normalizedCost)
                .eq(SysCreditAccountEntity::getUserId, userId)
                .ge(SysCreditAccountEntity::getBalance, normalizedCost));
            if (updated == 0) {
                SysCreditAccountEntity latest = accountForUpdate(session, userId, false);
                session.rollback();
                return ChargeResult.insufficient(latest == null ? 0 : defaultInt(latest.getBalance()));
            }
            SysCreditAccountEntity after = account(session, userId, true);
            int balanceAfter = after == null ? 0 : defaultInt(after.getBalance());
            int balanceBefore = balanceAfter + normalizedCost;
            String transactionId = recordTransaction(
                session,
                userId,
                normalizedFeatureCode,
                normalizedCost == 0 ? "USAGE" : "CONSUME",
                -normalizedCost,
                balanceBefore,
                balanceAfter,
                context
            );
            session.commit();
            return ChargeResult.charged(balanceBefore, balanceAfter, transactionId);
        }
    }

    public void refund(Long userId, String featureCode, int amount, CreditTransactionContext context) {
        int normalizedAmount = Math.max(0, amount);
        if (userId == null || normalizedAmount <= 0) {
            return;
        }
        try (SqlSession session = sqlSessionFactory.openSession(false)) {
            ensureAccount(session, userId, DEFAULT_INITIAL_BALANCE);
            SysCreditAccountEntity before = account(session, userId, true);
            int balanceBefore = before == null ? 0 : defaultInt(before.getBalance());
            int balanceAfter = balanceBefore + normalizedAmount;
            SysCreditAccountEntity update = new SysCreditAccountEntity();
            update.setBalance(balanceAfter);
            session.getMapper(SysCreditAccountMapper.class).update(
                update,
                Wrappers.<SysCreditAccountEntity>lambdaUpdate().eq(SysCreditAccountEntity::getUserId, userId)
            );
            recordTransaction(session, userId, featureCode, "REFUND", normalizedAmount, balanceBefore, balanceAfter, context);
            session.commit();
        }
    }

    public AdminCreditUserResponse adjust(Long userId, int amount, String reason) {
        if (userId == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        try (SqlSession session = sqlSessionFactory.openSession(false)) {
            SysUserEntity user = session.getMapper(SysUserMapper.class).selectById(userId);
            if (user == null) {
                throw new IllegalArgumentException("用户不存在");
            }
            if (isAdminUsername(user.getUsername())) {
                throw new IllegalArgumentException("admin 账号不参与积分调整");
            }
            ensureAccount(session, userId, DEFAULT_INITIAL_BALANCE);
            SysCreditAccountEntity before = account(session, userId, true);
            int balanceBefore = before == null ? 0 : defaultInt(before.getBalance());
            int balanceAfter = balanceBefore + amount;
            if (balanceAfter < 0) {
                throw new IllegalArgumentException("调整后积分不能小于 0");
            }
            SysCreditAccountEntity update = new SysCreditAccountEntity();
            update.setBalance(balanceAfter);
            update.setTotalAdjusted(defaultInt(before.getTotalAdjusted()) + amount);
            session.getMapper(SysCreditAccountMapper.class).update(
                update,
                Wrappers.<SysCreditAccountEntity>lambdaUpdate().eq(SysCreditAccountEntity::getUserId, userId)
            );
            recordTransaction(
                session,
                userId,
                "",
                "ADJUST",
                amount,
                balanceBefore,
                balanceAfter,
                new CreditTransactionContext("", "", "", reason, Map.of())
            );
            session.commit();
        }
        return listUsers("", Set.of(userId)).stream().findFirst().orElseThrow();
    }

    public List<AdminCreditUserResponse> listUsers(String keyword) {
        return listUsers(keyword, Set.of());
    }

    public List<AdminCreditTransactionResponse> listTransactions(Long userId) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            return session.getMapper(SysCreditTransactionMapper.class).selectList(
                Wrappers.<SysCreditTransactionEntity>lambdaQuery()
                    .eq(SysCreditTransactionEntity::getUserId, userId)
                    .orderByDesc(SysCreditTransactionEntity::getCreatedAt)
                    .last("LIMIT 200")
            ).stream().map(this::toTransactionResponse).toList();
        }
    }

    private List<AdminCreditUserResponse> listUsers(String keyword, Set<Long> onlyIds) {
        try (SqlSession session = sqlSessionFactory.openSession(false)) {
            SysUserMapper userMapper = session.getMapper(SysUserMapper.class);
            String normalizedKeyword = keyword == null ? "" : keyword.trim();
            List<SysUserEntity> users = userMapper.selectList(
                Wrappers.<SysUserEntity>lambdaQuery()
                    .ne(SysUserEntity::getUsername, "admin")
                    .in(onlyIds != null && !onlyIds.isEmpty(), SysUserEntity::getId, onlyIds)
                    .and(!normalizedKeyword.isBlank(), wrapper -> wrapper
                        .like(SysUserEntity::getUsername, normalizedKeyword.toLowerCase(Locale.ROOT))
                        .or()
                        .like(SysUserEntity::getDisplayName, normalizedKeyword))
                    .orderByDesc(SysUserEntity::getCreatedAt)
            );
            for (SysUserEntity user : users) {
                if (!UserStatus.ACTIVE.value().equals(user.getStatus())) {
                    ensureAccount(session, user.getId(), DEFAULT_INITIAL_BALANCE);
                    continue;
                }
                ensureAccount(session, user.getId(), DEFAULT_INITIAL_BALANCE);
            }
            session.commit();
            if (users.isEmpty()) {
                return List.of();
            }
            Map<Long, SysCreditAccountEntity> accountMap = accountMap(session, users.stream().map(SysUserEntity::getId).toList());
            Map<Long, UsageStats> usageStats = usageStats(session, users.stream().map(SysUserEntity::getId).toList());
            return users.stream()
                .map(user -> toUserResponse(user, accountMap.get(user.getId()), usageStats.get(user.getId())))
                .toList();
        }
    }

    private void ensureAccount(SqlSession session, Long userId, int initialBalance) {
        if (userId == null) {
            return;
        }
        SysUserEntity user = session.getMapper(SysUserMapper.class).selectById(userId);
        if (user == null || isAdminUsername(user.getUsername())) {
            return;
        }
        SysCreditAccountMapper accountMapper = session.getMapper(SysCreditAccountMapper.class);
        SysCreditAccountEntity existing = accountMapper.selectOne(
            Wrappers.<SysCreditAccountEntity>lambdaQuery()
                .eq(SysCreditAccountEntity::getUserId, userId)
                .last("LIMIT 1")
        );
        if (existing != null) {
            return;
        }
        SysCreditAccountEntity account = new SysCreditAccountEntity();
        account.setUserId(userId);
        account.setBalance(Math.max(0, initialBalance));
        account.setTotalConsumed(0);
        account.setTotalAdjusted(0);
        accountMapper.insert(account);
        session.flushStatements();
    }

    private SysCreditAccountEntity account(SqlSession session, Long userId, boolean required) {
        if (userId == null) {
            return null;
        }
        SysCreditAccountEntity account = session.getMapper(SysCreditAccountMapper.class).selectOne(
            Wrappers.<SysCreditAccountEntity>lambdaQuery()
                .eq(SysCreditAccountEntity::getUserId, userId)
                .last("LIMIT 1")
        );
        if (account == null && required) {
            throw new IllegalStateException("积分账户不存在");
        }
        return account;
    }

    private SysCreditAccountEntity accountForUpdate(SqlSession session, Long userId, boolean required) {
        if (userId == null) {
            return null;
        }
        SysCreditAccountEntity account = session.getMapper(SysCreditAccountMapper.class).selectOne(
            Wrappers.<SysCreditAccountEntity>lambdaQuery()
                .eq(SysCreditAccountEntity::getUserId, userId)
                .last("LIMIT 1 FOR UPDATE")
        );
        if (account == null && required) {
            throw new IllegalStateException("积分账户不存在");
        }
        return account;
    }

    private SysCreditRuleEntity findRule(SqlSession session, String featureCode) {
        String normalizedFeatureCode = CreditFeatureCode.normalize(featureCode);
        if (normalizedFeatureCode.isBlank()) {
            return null;
        }
        return session.getMapper(SysCreditRuleMapper.class).selectOne(
            Wrappers.<SysCreditRuleEntity>lambdaQuery()
                .eq(SysCreditRuleEntity::getFeatureCode, normalizedFeatureCode)
                .last("LIMIT 1")
        );
    }

    private Map<Long, SysCreditAccountEntity> accountMap(SqlSession session, List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, SysCreditAccountEntity> result = new LinkedHashMap<>();
        for (SysCreditAccountEntity account : session.getMapper(SysCreditAccountMapper.class).selectList(
            Wrappers.<SysCreditAccountEntity>lambdaQuery().in(SysCreditAccountEntity::getUserId, userIds)
        )) {
            result.put(account.getUserId(), account);
        }
        return result;
    }

    private Map<Long, UsageStats> usageStats(SqlSession session, List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, UsageStats> result = new LinkedHashMap<>();
        for (SysCreditTransactionEntity transaction : session.getMapper(SysCreditTransactionMapper.class).selectList(
            Wrappers.<SysCreditTransactionEntity>lambdaQuery()
                .in(SysCreditTransactionEntity::getUserId, userIds)
                .in(SysCreditTransactionEntity::getTransactionType, List.of("CONSUME", "USAGE"))
                .orderByAsc(SysCreditTransactionEntity::getCreatedAt)
        )) {
            UsageStats stats = result.computeIfAbsent(transaction.getUserId(), ignored -> new UsageStats());
            String featureCode = CreditFeatureCode.normalize(transaction.getFeatureCode());
            if (CreditFeatureCode.IMAGE_GENERATION.equals(featureCode)) {
                stats.imageGenerationCount += 1;
            }
            if (CreditFeatureCode.VIDEO_GENERATION.equals(featureCode)) {
                stats.videoGenerationCount += 1;
            }
            stats.lastUsedAt = transaction.getCreatedAt();
        }
        return result;
    }

    private String recordTransaction(
        SqlSession session,
        Long userId,
        String featureCode,
        String transactionType,
        int amountDelta,
        int balanceBefore,
        int balanceAfter,
        CreditTransactionContext context
    ) {
        SysCreditTransactionEntity entity = new SysCreditTransactionEntity();
        entity.setTransactionId("credit_" + UUID.randomUUID().toString().replace("-", ""));
        entity.setUserId(userId);
        entity.setFeatureCode(CreditFeatureCode.normalize(featureCode));
        entity.setTransactionType(transactionType);
        entity.setAmountDelta(amountDelta);
        entity.setBalanceBefore(balanceBefore);
        entity.setBalanceAfter(balanceAfter);
        entity.setRelatedRunId(context == null ? "" : trimToEmpty(context.runId()));
        entity.setRelatedTaskId(context == null ? "" : trimToEmpty(context.taskId()));
        entity.setRelatedWorkflowId(context == null ? "" : trimToEmpty(context.workflowId()));
        entity.setReason(context == null ? "" : trimToEmpty(context.reason()));
        entity.setMetadataJson(writeJson(context == null ? Map.of() : context.metadata()));
        session.getMapper(SysCreditTransactionMapper.class).insert(entity);
        return entity.getTransactionId();
    }

    private AdminCreditUserResponse toUserResponse(SysUserEntity user, SysCreditAccountEntity account, UsageStats stats) {
        return new AdminCreditUserResponse(
            user.getId(),
            user.getUsername(),
            user.getDisplayName(),
            user.getRole(),
            user.getStatus(),
            account == null ? 0 : defaultInt(account.getBalance()),
            account == null ? 0 : defaultInt(account.getTotalConsumed()),
            account == null ? 0 : defaultInt(account.getTotalAdjusted()),
            stats == null ? 0L : stats.imageGenerationCount,
            stats == null ? 0L : stats.videoGenerationCount,
            stats == null ? null : stats.lastUsedAt
        );
    }

    private AdminCreditRuleResponse toRuleResponse(SysCreditRuleEntity rule) {
        return new AdminCreditRuleResponse(
            rule == null ? "" : rule.getFeatureCode(),
            rule == null ? "" : rule.getDisplayName(),
            rule == null ? 0 : defaultInt(rule.getCost()),
            rule == null ? null : rule.getUpdatedAt()
        );
    }

    private AdminCreditTransactionResponse toTransactionResponse(SysCreditTransactionEntity transaction) {
        return new AdminCreditTransactionResponse(
            transaction.getTransactionId(),
            transaction.getUserId(),
            transaction.getFeatureCode(),
            transaction.getTransactionType(),
            defaultInt(transaction.getAmountDelta()),
            defaultInt(transaction.getBalanceBefore()),
            defaultInt(transaction.getBalanceAfter()),
            transaction.getRelatedRunId(),
            transaction.getRelatedTaskId(),
            transaction.getRelatedWorkflowId(),
            transaction.getReason(),
            readJson(transaction.getMetadataJson()),
            transaction.getCreatedAt()
        );
    }

    private String defaultRuleName(String featureCode) {
        return switch (CreditFeatureCode.normalize(featureCode)) {
            case CreditFeatureCode.IMAGE_GENERATION -> "图片生成";
            case CreditFeatureCode.VIDEO_GENERATION -> "视频生成";
            default -> featureCode;
        };
    }

    private boolean isAdminUsername(String username) {
        return "admin".equalsIgnoreCase(trimToEmpty(username));
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private String writeJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (Exception ex) {
            return "{}";
        }
    }

    private Map<String, Object> readJson(String value) {
        try {
            String normalized = trimToEmpty(value);
            return normalized.isBlank() ? Map.of() : objectMapper.readValue(normalized, MAP_TYPE);
        } catch (Exception ex) {
            return Map.of();
        }
    }

    public record CreditBalance(int balance, int totalConsumed, int totalAdjusted) {}

    public record ChargeResult(
        boolean success,
        int balanceBefore,
        int balanceAfter,
        String transactionId
    ) {
        static ChargeResult charged(int balanceBefore, int balanceAfter, String transactionId) {
            return new ChargeResult(true, balanceBefore, balanceAfter, transactionId);
        }

        static ChargeResult insufficient(int available) {
            return new ChargeResult(false, available, available, "");
        }
    }

    private static final class UsageStats {
        private long imageGenerationCount;
        private long videoGenerationCount;
        private OffsetDateTime lastUsedAt;
    }
}
