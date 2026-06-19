package com.cleancodecrew.sweg.repository;

import com.cleancodecrew.sweg.model.HorarioRegla;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HorarioReglaRepository extends JpaRepository<HorarioRegla, Long> {
    List<HorarioRegla> findByActivoTrue();
}
