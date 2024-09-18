package org.rozkladbot.repositories;

import org.rozkladbot.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    @Query(value = "select u from User u where u.chatID = ?1")
    User findByChatId(long chatId);
}
