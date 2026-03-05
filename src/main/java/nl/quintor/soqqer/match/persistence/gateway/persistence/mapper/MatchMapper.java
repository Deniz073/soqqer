package nl.quintor.soqqer.match.persistence.gateway.persistence.mapper;

import org.mapstruct.*;
import nl.quintor.soqqer.match.persistence.entity.Match;
import nl.quintor.soqqer.match.persistence.gateway.api.dto.CreateMatchDTO;
import nl.quintor.soqqer.match.persistence.gateway.api.dto.MatchDTO;


@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface MatchMapper {
    Match toEntity(CreateMatchDTO createMatchDTO);

    MatchDTO toDto(Match match);

    @AfterMapping
    default void linkPlayers(@MappingTarget Match match) {
        match.getPlayers().forEach(player -> player.setMatch(match));
    }

}