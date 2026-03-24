package nl.quintor.soqqer.employee.persistence.mapper;

import nl.quintor.soqqer.employee.EmployeeMTO;
import nl.quintor.soqqer.employee.gateway.api.dto.CreateEmployeeDTO;
import nl.quintor.soqqer.employee.gateway.api.dto.EmployeeDTO;
import nl.quintor.soqqer.employee.gateway.api.dto.EmployeeSelectDTO;
import nl.quintor.soqqer.employee.gateway.api.dto.UpdateEmployeeDTO;
import nl.quintor.soqqer.employee.persistence.entity.Employee;
import nl.quintor.soqqer.employee.persistence.entity.Office;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface EmployeeMapper {

    Employee toEntity(CreateEmployeeDTO dto);

    @Mapping(target = "office", source = "office", qualifiedByName = "officeToNormalizedName")
    EmployeeDTO toDTO(Employee employee);

    @Mapping(target = "office", source = "office", qualifiedByName = "officeToNormalizedName")
    EmployeeSelectDTO toSelectDTO(Employee employee);

    List<EmployeeSelectDTO> toSelectDTO(List<Employee> employees);

    Employee update(UpdateEmployeeDTO dto, @MappingTarget Employee employee);

    @Mapping(target = "office", source = "office", qualifiedByName = "officeToNormalizedName")
    EmployeeMTO toMTO(Employee employee);

    @Named("officeToNormalizedName")
    default String officeToNormalizedName(Office office) {
        return office == null ? null : office.getNormalizedName();
    }
}
