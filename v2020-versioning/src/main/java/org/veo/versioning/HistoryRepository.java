package org.veo.versioning;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface HistoryRepository extends CrudRepository<HistoryEntry, String> {

    @Query("SELECT h FROM HistoryEntry h WHERE h.dataId = :dataId")
    public List<HistoryEntry> findByDataId(@Param("dataId") String dataId);

}
