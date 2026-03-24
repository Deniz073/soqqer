package nl.quintor.soqqer.employee.persistence.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import nl.quintor.soqqer.common.BaseEntity;
import nl.quintor.soqqer.employee.EmployeeLookup;
import nl.quintor.soqqer.employee.EmployeeMTO;
import nl.quintor.soqqer.employee.gateway.api.dto.CreateEmployeeDTO;
import nl.quintor.soqqer.employee.gateway.api.dto.EmployeeDTO;
import nl.quintor.soqqer.employee.gateway.api.dto.EmployeeSelectDTO;
import nl.quintor.soqqer.employee.gateway.api.dto.UpdateEmployeeDTO;
import nl.quintor.soqqer.employee.persistence.entity.Employee;
import nl.quintor.soqqer.employee.persistence.exception.EmployeeAlreadyExistsException;
import nl.quintor.soqqer.employee.persistence.mapper.EmployeeMapper;
import nl.quintor.soqqer.employee.persistence.repository.EmployeeRepository;
import nl.quintor.soqqer.common.events.match.MatchFinishedEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmployeeService implements EmployeeLookup {
    private final EmployeeRepository employeeRepository;
    private final EmployeeMapper employeeMapper;
    private static final int K = 20;

    public Page<EmployeeDTO> find(Pageable pageable) {
        Sort sort = pageable.getSort().isSorted()
                ? pageable.getSort()
                : Sort.by(Sort.Direction.DESC, "elo");

        var pageReq = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                sort
        );

        var employeesPage = employeeRepository.findAll(pageReq);
        return employeesPage.map(employeeMapper::toDTO);
    }

    public List<EmployeeSelectDTO> findForSelect() {
        return employeeMapper.toSelectDTO(employeeRepository.findAll(Sort.by("name")));
    }

    public EmployeeDTO create(CreateEmployeeDTO dto) {
        if (employeeRepository.existsByNameAndOffice(dto.name(), dto.office())) {
            throw new EmployeeAlreadyExistsException(
                    "An employee with this name already exists in this office."
            );
        }

        var employee = employeeRepository.save(employeeMapper.toEntity(dto));
        return employeeMapper.toDTO(employee);
    }

    public EmployeeDTO findById(Long id) {
        return employeeRepository.findById(id)
                .map(employeeMapper::toDTO)
                .orElseThrow(() -> new EntityNotFoundException("Employee with id " + id + " was not found."));
    }

    public EmployeeDTO update(Long id, UpdateEmployeeDTO dto) {
        var employee = employeeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Employee with id " + id + " was not found."));

        if (employeeRepository.existsByNameAndOfficeAndIdNot(dto.name(), dto.office(), id)) {
            throw new EmployeeAlreadyExistsException(
                    "An employee with this name already exists in this office."
            );
        }

        var updatedEmployee = employeeMapper.update(dto, employee);
        var saved = employeeRepository.save(updatedEmployee);
        return employeeMapper.toDTO(saved);
    }

    public void delete(Long id) {
        var employee = employeeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Employee with id " + id + " was not found."));
        employeeRepository.delete(employee);
    }

    public void calculateNewPlayerElos(MatchFinishedEvent event) {
        Set<Long> allPlayerIds = new HashSet<>();
        allPlayerIds.addAll(event.teamOnePlayerIds());
        allPlayerIds.addAll(event.teamTwoPlayerIds());

        Map<Long, Employee> employees = employeeRepository.findAllById(allPlayerIds)
                .stream()
                .collect(Collectors.toMap(BaseEntity::getId, Function.identity()));

        List<Employee> teamOne = event.teamOnePlayerIds()
                .stream()
                .map(employees::get)
                .toList();

        List<Employee> teamTwo = event.teamTwoPlayerIds()
                .stream()
                .map(employees::get)
                .toList();

        double teamOneRating = teamOne.stream()
                .mapToInt(Employee::getElo)
                .average()
                .orElseThrow();

        double teamTwoRating = teamTwo.stream()
                .mapToInt(Employee::getElo)
                .average()
                .orElseThrow();

        double expectedTeamOne =
                1.0 / (1 + Math.pow(10, (teamTwoRating - teamOneRating) / 400));

        double expectedTeamTwo = 1 - expectedTeamOne;

        double actualTeamOne;
        double actualTeamTwo;

        if (event.teamOneScore() > event.teamTwoScore()) {
            actualTeamOne = 1;
            actualTeamTwo = 0;
        } else if (event.teamTwoScore() > event.teamOneScore()) {
            actualTeamOne = 0;
            actualTeamTwo = 1;
        } else {
            actualTeamOne = 0.5;
            actualTeamTwo = 0.5;
        }

        double deltaTeamOne = K * (actualTeamOne - expectedTeamOne);
        double deltaTeamTwo = K * (actualTeamTwo - expectedTeamTwo);

        teamOne.forEach(player ->
                player.setElo((int) Math.round(player.getElo() + deltaTeamOne)));

        teamTwo.forEach(player ->
                player.setElo((int) Math.round(player.getElo() + deltaTeamTwo)));

        employeeRepository.saveAll(employees.values());
    }

    public void handleCrawlCounter(MatchFinishedEvent event) {
        if (event.teamOneScore() >= 10 && event.teamTwoScore() == 0) {
            var teamTwoPlayers = employeeRepository.findAllById(event.teamTwoPlayerIds());

            teamTwoPlayers.forEach(employee -> employee.setCrawlCounter(employee.getCrawlCounter() + 1));

            employeeRepository.saveAll(teamTwoPlayers);
        } else if (event.teamTwoScore() >= 10 && event.teamOneScore() == 0) {
            var teamOnePlayers = employeeRepository.findAllById(event.teamOnePlayerIds());

            teamOnePlayers.forEach(employee -> employee.setCrawlCounter(employee.getCrawlCounter() + 1));

            employeeRepository.saveAll(teamOnePlayers);
        }
    }

    @Override
    public Set<Long> findMissingEmployeeIds(Set<Long> employeeIds) {
        if (employeeIds == null || employeeIds.isEmpty()) {
            return Set.of();
        }

        var existingIds = employeeRepository.findAllById(employeeIds).stream()
                .map(BaseEntity::getId)
                .collect(Collectors.toSet());

        return employeeIds.stream()
                .filter(employeeId -> !existingIds.contains(employeeId))
                .collect(Collectors.toSet());
    }

    @Override
    public Map<Long, EmployeeMTO> findEmployees(Set<Long> employeeIds) {
        var employees = employeeRepository.findAllById(employeeIds);

        return employees.stream().collect(Collectors.toMap(BaseEntity::getId, employeeMapper::toMTO));
    }
}
