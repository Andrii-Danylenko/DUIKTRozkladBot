package org.rozkladbot.services;

import org.rozkladbot.entities.User;
import org.rozkladbot.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public boolean create(User user) {
        try {
            userRepository.save(user);
            return true;
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return false;
        }
    }
    @Transactional
    public boolean delete(User user) {
        if (userRepository.existsById(user.getChatID())) {
            userRepository.delete(user);
            return true;
        }
        return false;
    }

    @Transactional
    public boolean update(User user) {
        if (userRepository.existsById(user.getChatID())) {
            create(user);
            return true;
        }
        return false;
    }

    public User findById(long chatId) {
        return userRepository.findByChatId(chatId);
    }
}
