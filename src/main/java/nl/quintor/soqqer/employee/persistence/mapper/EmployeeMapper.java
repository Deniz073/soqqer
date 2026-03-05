package nl.quintor.soqqer.employee.persistence.mapper;

import nl.quintor.soqqer.employee.EmployeeMTO;
import nl.quintor.soqqer.employee.gateway.api.dto.CreateEmployeeDTO;
import nl.quintor.soqqer.employee.gateway.api.dto.EmployeeDTO;
import nl.quintor.soqqer.employee.gateway.api.dto.UpdateEmployeeDTO;
import nl.quintor.soqqer.employee.persistence.entity.Employee;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface EmployeeMapper {

    Employee toEntity(CreateEmployeeDTO dto);

    EmployeeDTO toDTO(Employee employee);

    List<EmployeeDTO> toDTO(List<Employee> employees);

    Employee update(UpdateEmployeeDTO dto, @MappingTarget Employee employee);
    EmployeeMTO toMTO(Employee employee);

}
