package co.edu.unbosque.alquiler.repository;

import co.edu.unbosque.alquiler.entity.Categoria;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
    List<Categoria> findByEstado(String estado, Sort sort);
}

