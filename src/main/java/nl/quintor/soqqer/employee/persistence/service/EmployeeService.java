package nl.quintor.soqqer.employee.persistence.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import nl.quintor.soqqer.employee.gateway.api.dto.CreateEmployeeDTO;
import nl.quintor.soqqer.employee.gateway.api.dto.EmployeeDTO;
import nl.quintor.soqqer.employee.gateway.api.dto.UpdateEmployeeDTO;
import nl.quintor.soqqer.employee.persistence.exception.EmployeeAlreadyExistsException;
import nl.quintor.soqqer.employee.persistence.mapper.EmployeeMapper;
import nl.quintor.soqqer.employee.persistence.repository.EmployeeRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmployeeService {
    private final EmployeeRepository employeeRepository;
    private final EmployeeMapper employeeMapper;

    public List<EmployeeDTO> findAll() {
        return employeeMapper.toDTO(employeeRepository.findAll());
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
}
