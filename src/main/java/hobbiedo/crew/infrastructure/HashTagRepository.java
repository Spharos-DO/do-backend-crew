package hobbiedo.crew.infrastructure;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import hobbiedo.crew.domain.HashTag;

public interface HashTagRepository extends JpaRepository<HashTag, Long> {
	@Query("select h.name from HashTag h where h.crew.id = :crewId")
	List<String> findNamesByCrewId(Long crewId);

	void deleteByCrewId(Long crewId);
}
