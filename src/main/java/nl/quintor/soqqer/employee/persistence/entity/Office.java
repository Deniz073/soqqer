package nl.quintor.soqqer.employee.persistence.entity;

import lombok.Getter;

@Getter
public enum Office {
    DENBOSCH("Den Bosch"),
    DEVENTER("Deventer"),
    GRONINGEN("Groningen"),
    DENHAAG("Den Haag"),
    AMERSFOORT("Amersfoort");

    private String normalizedName;

    Office(String normalizedName) {
        this.normalizedName = normalizedName;
    }
}
