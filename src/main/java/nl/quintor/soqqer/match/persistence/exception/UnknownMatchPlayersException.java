package nl.quintor.soqqer.match.persistence.exception;

import java.util.Set;

public class UnknownMatchPlayersException extends RuntimeException {
    private final Set<Long> missingEmployeeIds;

    public UnknownMatchPlayersException(Set<Long> missingEmployeeIds) {
        super("One or more player employeeIds do not exist: " + missingEmployeeIds);
        this.missingEmployeeIds = Set.copyOf(missingEmployeeIds);
    }

    public Set<Long> getMissingEmployeeIds() {
        return missingEmployeeIds;
    }
}
