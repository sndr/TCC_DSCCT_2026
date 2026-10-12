package br.tcc.comparador.web;

import br.tcc.comparador.config.AppProperties;
import br.tcc.comparador.domain.Metodo;
import br.tcc.comparador.service.BenchmarkService;
import br.tcc.comparador.service.ComparacaoService;
import br.tcc.comparador.web.Dtos.BenchmarkResponse;
import br.tcc.comparador.web.Dtos.ComparacaoResponse;
import br.tcc.comparador.web.Dtos.HistoricoItem;
import br.tcc.comparador.web.Dtos.StatusResponse;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class ComparacaoController {

    private final ComparacaoService comparacaoService;
    private final BenchmarkService benchmarkService;
    private final AppProperties properties;

    public ComparacaoController(ComparacaoService comparacaoService,
                                BenchmarkService benchmarkService,
                                AppProperties properties) {
        this.comparacaoService = comparacaoService;
        this.benchmarkService = benchmarkService;
        this.properties = properties;
    }

    @GetMapping("/status")
    public StatusResponse status() {
        return new StatusResponse(properties.embeddings().enabled(), properties.embeddings().model());
    }

    @PostMapping(value = "/comparacoes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ComparacaoResponse comparar(@RequestParam("arquivoA") MultipartFile arquivoA,
                                       @RequestParam("arquivoB") MultipartFile arquivoB,
                                       @RequestParam(value = "metodo", defaultValue = "AMBOS") Metodo metodo) {
        return comparacaoService.comparar(arquivoA, arquivoB, metodo);
    }

    @GetMapping("/comparacoes")
    public List<HistoricoItem> historico() {
        return comparacaoService.historico();
    }

    @PostMapping(value = "/benchmark", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BenchmarkResponse benchmark(@RequestParam("arquivos") List<MultipartFile> arquivos) {
        return benchmarkService.executar(arquivos);
    }
}
