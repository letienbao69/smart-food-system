package com.food.smart_food_system.Config;

import com.food.smart_food_system.Entity.RoleEntity;
import com.food.smart_food_system.Entity.UserEntity;
import com.food.smart_food_system.Repository.RoleRepository;
import com.food.smart_food_system.Repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Seeds the two required roles (ADMIN / CUSTOMER) and a bootstrap admin
 * account if no admin exists yet. The admin password is taken from the
 * bootstrap.admin.password property (env var BOOTSTRAP_ADMIN_PASSWORD).
 *
 * In dev you can just rely on the default "Admin@123" - in production
 * ALWAYS set BOOTSTRAP_ADMIN_PASSWORD before first start.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${bootstrap.admin.email:admin@foodshop.local}")
    private String adminEmail;

    @Value("${bootstrap.admin.password:Admin@123}")
    private String adminPassword;

    @Value("${bootstrap.admin.full-name:System Administrator}")
    private String adminFullName;

    public DataSeeder(RoleRepository roleRepository,
                      UserRepository userRepository,
                      PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        RoleEntity adminRole = createRoleIfMissing("ADMIN");
        createRoleIfMissing("CUSTOMER");

        boolean hasAdmin = userRepository.findAll().stream()
                .anyMatch(u -> u.getRoles() != null
                        && u.getRoles().stream().anyMatch(r -> "ADMIN".equals(r.getName())));

        if (!hasAdmin) {
            UserEntity admin = new UserEntity();
            admin.setFullName(adminFullName);
            admin.setEmail(adminEmail);
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setStatus("ACTIVE");
            admin.setRoles(Set.of(adminRole));
            userRepository.save(admin);
            log.warn("====================================================================");
            log.warn("Bootstrap admin created: {} / {} (CHANGE THIS PASSWORD IMMEDIATELY)",
                    adminEmail, adminPassword);
            log.warn("====================================================================");
        }
    }

    private RoleEntity createRoleIfMissing(String name) {
        return roleRepository.findByName(name).orElseGet(() -> {
            RoleEntity role = new RoleEntity();
            role.setName(name);
            return roleRepository.save(role);
        });
    }
}
