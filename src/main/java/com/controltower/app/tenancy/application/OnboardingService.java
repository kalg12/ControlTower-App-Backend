package com.controltower.app.tenancy.application;

import com.controltower.app.tenancy.api.dto.OnboardingRequest;
import com.controltower.app.identity.api.dto.UserResponse;
import com.controltower.app.identity.domain.PermissionRepository;
import com.controltower.app.identity.domain.Role;
import com.controltower.app.identity.domain.RoleRepository;
import com.controltower.app.identity.domain.Tenant;
import com.controltower.app.identity.domain.TenantRepository;
import com.controltower.app.identity.domain.User;
import com.controltower.app.identity.domain.UserRepository;
import com.controltower.app.identity.mapper.UserMapper;
import com.controltower.app.licenses.domain.License;
import com.controltower.app.licenses.domain.LicenseRepository;
import com.controltower.app.licenses.domain.Plan;
import com.controltower.app.licenses.domain.PlanRepository;
import com.controltower.app.shared.exception.ControlTowerException;
import com.controltower.app.shared.exception.ResourceNotFoundException;
import com.controltower.app.tenancy.api.dto.OnboardingResponse;
import com.controltower.app.tenancy.api.dto.TenantResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class OnboardingService {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final LicenseRepository licenseRepository;
    private final PlanRepository planRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    @Transactional
    public OnboardingResponse onboard(OnboardingRequest request) {
        // 1. Validate slug uniqueness
        if (tenantRepository.existsBySlugAndDeletedAtIsNull(request.getTenantSlug())) {
            throw new ControlTowerException(
                "Tenant slug already in use: " + request.getTenantSlug(), HttpStatus.CONFLICT);
        }

        // 2. Create Tenant
        Tenant tenant = new Tenant();
        tenant.setName(request.getTenantName());
        tenant.setSlug(request.getTenantSlug());
        tenant = tenantRepository.save(tenant);

        // 3. Create Admin User
        if (userRepository.existsByEmailAndTenantIdAndDeletedAtIsNull(
                request.getAdminEmail(), tenant.getId())) {
            throw new ControlTowerException(
                "Email already in use: " + request.getAdminEmail(), HttpStatus.CONFLICT);
        }
        User user = new User();
        user.setTenant(tenant);
        user.setEmail(request.getAdminEmail());
        user.setFullName(request.getAdminFullName());
        user.setPasswordHash(passwordEncoder.encode(request.getAdminPassword()));
        user.setStatus(User.UserStatus.ACTIVE);
        user.setSuperAdmin(false);
        user = userRepository.save(user);

        // 4. Create default Admin Role for the tenant with all permissions
        Role adminRole = new Role();
        adminRole.setTenant(tenant);
        adminRole.setName("Admin");
        adminRole.setCode("ADMIN");
        adminRole.setDescription("Default admin role with all permissions");
        adminRole.setSystem(false);
        adminRole.getPermissions().addAll(permissionRepository.findAll());
        adminRole = roleRepository.save(adminRole);

        // 5. Assign role to user
        user.getRoles().add(adminRole);
        user = userRepository.save(user);

        // 6. Create Trial License using the "Trial" plan
        Plan trialPlan = planRepository.findAll().stream()
                .filter(p -> "Trial".equalsIgnoreCase(p.getName()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Plan", "Trial"));

        License license = new License();
        license.setTenantId(tenant.getId());
        // clientId is null for the tenant-level trial license created at onboarding
        license.setPlan(trialPlan);
        license.setStatus(License.LicenseStatus.TRIAL);
        license.setCurrentPeriodEnd(Instant.now().plus(14, ChronoUnit.DAYS));
        license = licenseRepository.save(license);

        log.info("Onboarded tenant {} with admin {} and license {}", tenant.getId(), user.getId(), license.getId());

        TenantResponse tenantResponse = TenantResponse.builder()
                .id(tenant.getId())
                .name(tenant.getName())
                .slug(tenant.getSlug())
                .status(tenant.getStatus().name())
                .createdAt(tenant.getCreatedAt())
                .build();

        UserResponse userResponse = userMapper.toResponse(user);

        return OnboardingResponse.builder()
                .tenant(tenantResponse)
                .user(userResponse)
                .licenseId(license.getId())
                .build();
    }
}
