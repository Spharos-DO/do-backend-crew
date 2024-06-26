package hobbiedo.crew.infrastructure;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import hobbiedo.crew.domain.JoinForm;

public interface JoinFormRepository extends CrudRepository<JoinForm, String> {
	boolean existsByCrewIdAndUuid(Long crewId, String uuid);

	List<JoinForm> findByCrewId(Long crewId);

	List<JoinForm> findByUuid(String uuid);
}


