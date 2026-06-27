package in.hridaykh.formbox.repository;

import in.hridaykh.formbox.model.Todo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;


@Repository
public interface TodoRepository extends JpaRepository<Todo, UUID> {

//	List<Todo> findByTitleContainingIgnoreCase(String keyword);

}