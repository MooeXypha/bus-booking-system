package com.xypha.onlineBus.account.users.service;


import com.xypha.onlineBus.account.role.Role;
import com.xypha.onlineBus.account.users.dto.UserRequest;
import com.xypha.onlineBus.account.users.dto.UserResponse;
import com.xypha.onlineBus.account.users.entity.User;
import com.xypha.onlineBus.account.users.mapper.UserMapper;
import com.xypha.onlineBus.account.users.mapper.UserMapperUtil;
import org.apache.coyote.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService implements UserDetailsService {


    private final UserMapper userMapper;

    private final PasswordEncoder encoder;

    public UserService (UserMapper userMapper, PasswordEncoder encoder){
        this.userMapper = userMapper;
        this.encoder = encoder;
    }

    public UserResponse createUser (UserRequest request){
        //Check If email is already taken
        if (userMapper.findByPhoneNumber(request.getGmail()) != null){
            throw new RuntimeException("Email already exists");
        }
        else if (userMapper.findByPhoneNumber(request.getPhoneNumber()) != null){
            throw new RuntimeException("Phone Number already exits");
        }
        else if (userMapper.findByNrc(request.getNrc()) != null){
            throw new RuntimeException("NRC already exists");
        }

        Role role = request.getRole() != null ? request.getRole() : Role.USER;
        if (role == Role.USER) {
            if (request.getUsername() == null ||
                    request.getGmail() == null ||
                    request.getPhoneNumber() == null ||
                    request.getNrc() == null ||
                    request.getDob() == null ||
                    request.getCitizenship() == null) {
                throw new RuntimeException("All personal details are required for regular users");
            }
        }
        if (request.getPassword() == null || request.getPassword().isEmpty()) {
            throw new RuntimeException("Password is required");
        }

            User user = UserMapperUtil.toEntity(request);
            user.setRole(role);
            user.setPassword(encoder.encode(user.getPassword()));

            userMapper.insertUser(user);
            return UserMapperUtil.toDTO(userMapper.getUserById(user.getId()));
    }

    public UserResponse getUserById(Long id){
        User user = userMapper.getUserById(id);
        return user != null ? UserMapperUtil.toDTO(user): null;
    }

    public UserResponse updateUser (Long id, UserRequest request){
        User existsUser = userMapper.getUserById(id);
        if (existsUser == null){
            return null;
        }
        existsUser.setUsername(request.getUsername());
        existsUser.setGmail(request.getGmail());
        existsUser.setPhoneNumber(request.getPhoneNumber());
        existsUser.setNrc(request.getNrc());
        existsUser.setGender(request.getGender());
        existsUser.setDob(request.getDob());
        existsUser.setCitizenship(request.getCitizenship());

        //update password
        if (request.getPassword() != null && !request.getPassword().isEmpty()){
            existsUser.setPassword(encoder.encode(request.getPassword()));
        }

        //update role
        if (request.getRole() != null){
            existsUser.setRole(request.getRole());
        }
        userMapper.updateUser(existsUser);
        return UserMapperUtil.toDTO(userMapper.getUserById(id));
    }

    public boolean deleteUser(Long id) {
        User user = userMapper.getUserById(id);
        if (user == null)
            return false;

            userMapper.deleteUser(id);
            return true;
        }


    public List<UserResponse> getAllUser(){
        return userMapper.getAllUser()
                .stream()
                .map(UserMapperUtil::toDTO)
                .collect(Collectors.toList());
    }

    public UserResponse getUserByUsername (String username){
        User user = userMapper.findByUsername(username);
        if (user == null)return null;
        return UserMapperUtil.toDTO(user);
    }
    public UserResponse updateUserByUsername (String username, UserRequest userRequest){
        User existsUser = userMapper.findByUsername(username);

        if (existsUser == null){
            throw new RuntimeException("User Not found");
        }
        existsUser.setUsername(userRequest.getUsername() != null ? userRequest.getUsername() : existsUser.getUsername());
        existsUser.setGmail(userRequest.getGmail() != null ? userRequest.getGmail() : existsUser.getGmail());
        existsUser.setPhoneNumber(userRequest.getPhoneNumber() != null ? userRequest.getPhoneNumber() : existsUser.getPhoneNumber());
        existsUser.setNrc(userRequest.getNrc() != null ? userRequest.getNrc() : existsUser.getNrc());
        existsUser.setGender(userRequest.getGender() != null ? userRequest.getGender() : existsUser.getGender());
        existsUser.setDob(userRequest.getDob() != null ? userRequest.getDob() : existsUser.getDob());
        existsUser.setCitizenship(userRequest.getCitizenship() != null ? userRequest.getCitizenship() : existsUser.getCitizenship());

        if (userRequest.getPassword() != null && !userRequest.getPassword().isEmpty() ){
            existsUser.setPassword(encoder.encode(userRequest.getPassword()));
        }
        userMapper.updateUser(existsUser);
        return UserMapperUtil.toDTO(existsUser);

    }





    @Override
    public UserDetails loadUserByUsername (String username) throws UsernameNotFoundException {
        User user = userMapper.findByUsername(username);
        System.out.println(username);
        System.out.println("DB password: " + user.getPassword());

        if (user == null) {
            throw new UsernameNotFoundException("User not found!");
        }
        return new CustomUserDetails(user);

    }
}
