package br.com.servicosja.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.servicoja.backend.dto.DisponibilidadeRequest;
import br.com.servicoja.backend.dto.DisponibilidadeResponse;
import br.com.servicoja.backend.dto.PrestadorServicoRequest;
import br.com.servicoja.backend.dto.PrestadorServicoResponse;
import br.com.servicoja.backend.exception.RecursoNaoEncontradoException;
import br.com.servicoja.backend.exception.RegraNegocioException;
import br.com.servicoja.backend.model.Disponibilidade;
import br.com.servicoja.backend.model.Prestador;
import br.com.servicoja.backend.model.PrestadorServico;
import br.com.servicoja.backend.model.Servico;
import br.com.servicoja.backend.model.enums.DiaSemana;
import br.com.servicoja.backend.repository.PrestadorRepository;
import br.com.servicoja.backend.repository.ServicoRepository;

/** Configurações do prestador: serviços que oferece, agenda semanal e raio de atuação. */
@Service

public class PrestadorService {
	rivate final PrestadorRepository prestadorRepository;
    private final ServicoRepository servicoRepository;

    public PrestadorService(PrestadorRepository prestadorRepository, ServicoRepository servicoRepository) {
        this.prestadorRepository = prestadorRepository;
        this.servicoRepository = servicoRepository;
    }

    // ---------------- Serviços oferecidos ----------------

    @Transactional(readOnly = true)
    public List<PrestadorServicoResponse> listarServicos(String email) {
        return buscar(email).getServicos().stream().map(this::toResponse).toList();
    }

    @Transactional
    public PrestadorServicoResponse adicionarServico(String email, PrestadorServicoRequest request) {
        Prestador prestador = buscar(email);
        Servico servico = servicoRepository.findById(request.servicoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Serviço não encontrado."));

        boolean jaOferece = prestador.getServicos().stream()
                .anyMatch(ps -> ps.getServico().getId().equals(servico.getId()));
        if (jaOferece) {
            throw new RegraNegocioException("Você já oferece este serviço.");
        }

        PrestadorServico oferta = new PrestadorServico();
        oferta.setPrestador(prestador);
        oferta.setServico(servico);
        oferta.setPrecoBase(request.precoBase());
        oferta.setAtivo(true);
        prestador.getServicos().add(oferta);   // gravado junto com o prestador (cascade)

        return toResponse(oferta);
    }

    @Transactional
    public void removerServico(String email, Long servicoId) {
        Prestador prestador = buscar(email);
        boolean removeu = prestador.getServicos()
                .removeIf(ps -> ps.getServico().getId().equals(servicoId));
        if (!removeu) {
            throw new RecursoNaoEncontradoException("Você não oferece este serviço.");
        }
    }

    // ---------------- Agenda semanal ----------------

    @Transactional(readOnly = true)
    public List<DisponibilidadeResponse> listarDisponibilidade(String email) {
        return buscar(email).getDisponibilidades().stream().map(this::toResponse).toList();
    }

    /** Substitui toda a agenda. Valida: início antes do fim e sem horários sobrepostos no mesmo dia. */
    @Transactional
    public List<DisponibilidadeResponse> definirDisponibilidade(String email, List<DisponibilidadeRequest> horarios) {
        Prestador prestador = buscar(email);
        validarHorarios(horarios);

        prestador.getDisponibilidades().clear();
        for (DisponibilidadeRequest h : horarios) {
            Disponibilidade d = new Disponibilidade();
            d.setPrestador(prestador);
            d.setDiaSemana(h.diaSemana());
            d.setHoraInicio(h.horaInicio());
            d.setHoraFim(h.horaFim());
            prestador.getDisponibilidades().add(d);
        }
        return prestador.getDisponibilidades().stream().map(this::toResponse).toList();
    }

    private void validarHorarios(List<DisponibilidadeRequest> horarios) {
        for (DisponibilidadeRequest h : horarios) {
            if (!h.horaInicio().isBefore(h.horaFim())) {
                throw new RegraNegocioException("A hora de início deve ser anterior à hora de fim.");
            }
        }

        Map<DiaSemana, List<DisponibilidadeRequest>> porDia = horarios.stream()
                .collect(Collectors.groupingBy(DisponibilidadeRequest::diaSemana));

        for (List<DisponibilidadeRequest> doDia : porDia.values()) {
            List<DisponibilidadeRequest> ordenados = new ArrayList<>(doDia);
            ordenados.sort(Comparator.comparing(DisponibilidadeRequest::horaInicio));
            for (int i = 1; i < ordenados.size(); i++) {
                if (ordenados.get(i).horaInicio().isBefore(ordenados.get(i - 1).horaFim())) {
                    throw new RegraNegocioException("Há horários sobrepostos no mesmo dia.");
                }
            }
        }
    }

    // ---------------- Raio de atuação ----------------

    @Transactional
    public void atualizarRaio(String email, double raioKm) {
        buscar(email).setRaioAtuacaoKm(raioKm);
    }

    // ---------------- Auxiliares ----------------

    private Prestador buscar(String email) {
        return prestadorRepository.findByEmail(email)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Prestador não encontrado."));
    }

    private PrestadorServicoResponse toResponse(PrestadorServico ps) {
        Servico s = ps.getServico();
        return new PrestadorServicoResponse(s.getId(), s.getNome(), s.getCategoria().getNome(), ps.getPrecoBase());
    }

    private DisponibilidadeResponse toResponse(Disponibilidade d) {
        return new DisponibilidadeResponse(d.getDiaSemana(), d.getHoraInicio(), d.getHoraFim());
    }
}