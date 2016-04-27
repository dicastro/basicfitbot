package es.qopuir.basicfitbot.repo;

import org.springframework.data.repository.CrudRepository;

import es.qopuir.basicfitbot.model.Chat;

public interface ChatRepository extends CrudRepository<Chat, Integer> {
}