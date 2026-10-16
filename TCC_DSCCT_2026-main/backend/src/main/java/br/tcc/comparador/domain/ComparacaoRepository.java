package br.tcc.comparador.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComparacaoRepository extends JpaRepository<Comparacao, Long> {

    List<Comparacao> findTop30ByOrderByCriadoEmDesc();
}
