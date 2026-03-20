package com.revpay.service;

import com.revpay.exception.ResourceNotFoundException;
import com.revpay.model.entity.BusinessProfile;
import com.revpay.model.entity.Role;
import com.revpay.model.entity.Transaction;
import com.revpay.model.entity.User;
import com.revpay.repository.BusinessProfileRepository;
import com.revpay.repository.TransactionRepository;
import com.revpay.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final BusinessProfileRepository businessProfileRepository;
    private final com.revpay.repository.WalletRepository walletRepository;

    // --- USER MANAGEMENT ---

    @Transactional(readOnly = true)
    public Page<User> getAllUsers(Pageable pageable) {
        log.info("Admin fetching all users (Page: {}, Size: {})", pageable.getPageNumber(), pageable.getPageSize());
        return userRepository.findAll(pageable);
    }

    @Transactional
    public User updateUserStatus(Long userId, boolean isActive) {
        log.info("Admin changing status of user {} to {}", userId, isActive ? "ACTIVE" : "INACTIVE");
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        // Protect the ADMIN role from being deactivated
        if (user.getRole() == Role.ADMIN) {
            throw new IllegalStateException("Cannot change the status of an ADMIN user via this endpoint.");
        }

        user.setActive(isActive);
        return userRepository.save(user);
    }

    // --- BUSINESS VERIFICATION LOGIC ---

    @Transactional
    public void verifyBusinessAccount(Long profileId) {
        log.info("Admin verifying business profile ID: {}", profileId);

        // 1. Fetch the business profile
        BusinessProfile profile = businessProfileRepository.findById(profileId)
                .orElseThrow(() -> new ResourceNotFoundException("Business profile not found with ID: " + profileId));

        if (profile.isVerified()) {
            throw new IllegalArgumentException("Business is already verified.");
        }

        // 2. Verify the Business Profile
        profile.setVerified(true);
        businessProfileRepository.save(profile);

        // 3. Fetch the underlying User
        User user = profile.getUser();

        // 4. Mark the User as VERIFIED and ACTIVE
        user.setVerified(true);
        user.setActive(true);
        userRepository.save(user);

        log.info("Successfully verified Business Profile ID: {} and activated User ID: {}", profileId, user.getUserId());
    }

    // --- TRANSACTION MONITORING ---

    @Transactional(readOnly = true)
    public Page<Transaction> getAllPlatformTransactions(Pageable pageable) {
        log.info("Admin fetching the global transaction ledger (Page: {}, Size: {})", pageable.getPageNumber(),
                pageable.getPageSize());
        return transactionRepository.findAll(pageable);
    }

    // --- SYSTEM ANALYTICS ---

    @Transactional(readOnly = true)
    public Map<String, Object> getSystemAnalytics() {
        log.info("Admin requested platform-wide analytics");

        long totalUsers = userRepository.count();
        long activeBusinesses = businessProfileRepository.count();
        long totalTransactions = transactionRepository.count();

        // Calculate total transaction volume
        BigDecimal estimatedVolume = transactionRepository.findAll().stream()
                .filter(t -> t.getStatus() == Transaction.TransactionStatus.COMPLETED)
                .filter(t -> t.getType() == Transaction.TransactionType.SEND
                        || t.getType() == Transaction.TransactionType.INVOICE_PAYMENT)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate Admin Wallet Balance
        BigDecimal adminWalletBalance = BigDecimal.ZERO;
        User adminUser = userRepository.findByRole(Role.ADMIN).stream().findFirst().orElse(null);

        if (adminUser != null) {
            com.revpay.model.entity.Wallet adminWallet = adminUser.getWallet();
            if (adminWallet != null) {
                adminWalletBalance = adminWallet.getBalance();
            } else {
                adminWalletBalance = walletRepository.findByUser(adminUser)
                        .map(com.revpay.model.entity.Wallet::getBalance)
                        .orElse(BigDecimal.ZERO);
            }
        }

        Map<String, Object> analytics = new HashMap<>();
        analytics.put("totalUsers", totalUsers);
        analytics.put("activeBusinesses", activeBusinesses);
        analytics.put("totalTransactions", totalTransactions);
        analytics.put("totalVolume", estimatedVolume);
        analytics.put("adminWalletBalance", adminWalletBalance);

        return analytics;
    }
}