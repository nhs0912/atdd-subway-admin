package nextstep.subway.line.domain;

import nextstep.subway.exception.InputDataErrorCode;
import nextstep.subway.exception.InputDataErrorException;
import nextstep.subway.station.domain.Station;

import javax.persistence.CascadeType;
import javax.persistence.Embeddable;
import javax.persistence.OneToMany;
import java.util.*;
import java.util.stream.Collectors;

@Embeddable
public class Sections {

    @OneToMany(mappedBy = "line", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Section> sections = new ArrayList<>();

    public List<Station> getStations() {
        return this.sections.stream()
                .map(Section::getStations)
                .flatMap(Collection::stream)
                .distinct()
                .collect(Collectors.toList());
    }

    public List<Section> getOrderedSection() {
        List<Section> orderedSections = new ArrayList<>();
        Section foundSection = findFirstSection();

        while (hasSection(foundSection)) {
            orderedSections.add(foundSection);
            foundSection = findNextSection(foundSection);
        }

        return orderedSections;
    }

    private boolean hasSection(Section foundSection) {
        return Optional.ofNullable(foundSection).isPresent();
    }

    private boolean isEmpty(Section foundSection) {
        return Objects.isNull(foundSection);
    }

    public List<Station> getOrderedStation() {
        return getOrderedSection().stream()
                .map(Section::getStations)
                .flatMap(Collection::stream)
                .distinct()
                .collect(Collectors.toList());
    }

    private Section findNextSection(Section section) {
        return this.sections.stream()
                .filter(it -> it.getUpStation() == section.getDownStation())
                .findFirst()
                .orElse(null);
    }

    private Section findFirstSection() {
        Station firstStation = findFirstStation();
        return this.sections.stream()
                .filter(it -> it.getUpStation() == firstStation)
                .findFirst()
                .orElseThrow(
                        () -> new InputDataErrorException(InputDataErrorCode.THERE_IS_NOT_SEARCHED_SECTION)
                );
    }

    public Station findFirstStation() {
        List<Station> upStations = getUpStations();
        List<Station> downStations = getDownStations();

        return upStations.stream()
                .filter(it -> !hasStation(it, downStations))
                .findFirst()
                .orElseThrow(
                        ()-> new InputDataErrorException(InputDataErrorCode.THERE_IS_NOT_SEARCHED_STATION)
                );
    }

    public Station findLastStation() {
        List<Station> upStations = getUpStations();
        List<Station> downStations = getDownStations();

        return downStations.stream()
                .filter(it -> !hasStation(it, upStations))
                .findFirst()
                .orElseThrow(
                        ()-> new InputDataErrorException(InputDataErrorCode.THERE_IS_NOT_SEARCHED_STATION)
                );
    }

    private boolean hasStation(Station station, List<Station> stations) {
        return stations.contains(station);
    }

    private List<Station> getDownStations() {
        return this.getSections().stream()
                .map(Section::getDownStation)
                .collect(Collectors.toList());
    }

    private List<Station> getUpStations() {
        return this.getSections().stream()
                .map(Section::getUpStation)
                .collect(Collectors.toList());
    }

    public void updateSection(Section newSection) {
        checkValidSection(newSection);
        addSection(newSection);
    }

    public void addSection(Section newSection) {
        for (Section section : sections) {
            section.addInnerSection(newSection);
        }
        sections.add(newSection);
    }

    private void checkValidSection(Section section) {
        if (isExist(section)) {
            throw new InputDataErrorException(InputDataErrorCode.THE_SECTION_ALREADY_EXISTS);
        }
        if (hasBothStationInLine(section)) {
            throw new InputDataErrorException(InputDataErrorCode.THE_STATIONS_ALREADY_EXISTS);
        }
        if (isFindNoOneStationInLine(section)) {
            throw new InputDataErrorException(InputDataErrorCode.THERE_IS_NOT_SEARCHED_STATION);
        }
    }

    private boolean hasBothStationInLine(Section section) {
        List<Station> stations = this.getStations();
        return stations.stream()
                .allMatch(it -> isSameAnyStationInSection(section, it));
    }

    private boolean isFindNoOneStationInLine(Section section) {
        List<Station> stations = this.getStations();
        return stations.stream()
                .noneMatch(it -> isSameAnyStationInSection(section, it));
    }

    private boolean isExist(Section section) {
        return this.sections.stream()
                .anyMatch(it -> isSameAnyStationInSection(section, it));
    }

    private boolean isSameAnyStationInSection(Section section, Station it) {
        return it.equals(section.getUpStation()) || it.equals(section.getDownStation());
    }

    private boolean isSameAnyStationInSection(Section section, Section it) {
        return it.equals(section);
    }

    public List<Section> getSections() {
        return sections;
    }

    public void removeStation(Station station) {
        if(isEndStation(station)){
            this.sections.remove(station);
        }

        if (!isEndStation(station)) {
            Section foundSectionByDownStation = this.sections.stream()
                    .filter(it -> it.getDownStation() == station)
                    .findFirst()
                    .get();

            Section foundSectionByUpStation = this.sections.stream()
                    .filter(it -> it.getUpStation() == station)
                    .findFirst()
                    .get();
            Distance newDistance = foundSectionByDownStation.getDistance().sum(foundSectionByUpStation.getDistance());
            Section changeSection = new Section(foundSectionByUpStation.getLine(),
                    foundSectionByDownStation.getUpStation(), foundSectionByUpStation.getDownStation(), newDistance);
            this.sections.add(changeSection);
            this.sections.removeAll(Arrays.asList(foundSectionByUpStation, foundSectionByDownStation));
        }
    }

    private boolean isEndStation(Station station) {
        return isFirstSection(station) || isLastSection(station);
    }

    private boolean isFirstSection(Station station) {
        return findFirstStation().equals(station);
    }

    private boolean isLastSection(Station station) {
        return findLastStation().equals(station);
    }
}
