package com.food.smart_food_system.Controller;


import com.food.smart_food_system.Config.JwtTokenProvider;
import com.food.smart_food_system.DTO.userDTO;
import com.food.smart_food_system.Entity.UserEntity;
import com.food.smart_food_system.Reponse.AuthResponse;
import com.food.smart_food_system.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private UserRepository userRepository; // 👈 thêm

    @PostMapping("/login")
    public AuthResponse login(@RequestBody userDTO request) {

        //  Xác thực người dùng
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        // Lấy user từ DB
        UserEntity user = userRepository.findByEmail(request.getEmail());


        //  Tạo token
        String token = tokenProvider.generateToken(user.getEmail());

        // Trả full info
        return new AuthResponse(
                token,
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                user.getStatus()
//                user.getRole().getName()
                // tạm thời cmt đã vì chưa có role
        );
    }
}