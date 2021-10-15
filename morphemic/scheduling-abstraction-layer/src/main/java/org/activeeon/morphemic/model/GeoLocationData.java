package org.activeeon.morphemic.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;


@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class GeoLocationData {
    private String city;

    private String country;

    private Double latitude;

    private Double longitude;

    private String region;

    private String cloud;

    @Override
    public boolean equals(java.lang.Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        GeoLocationData geoLocation = (GeoLocationData) o;
        return Objects.equals(this.cloud, geoLocation.cloud) &&
                Objects.equals(this.region, geoLocation.region);
    }

    @Override
    public String toString() {
        return "GeoLocationData{" +
                "city='" + city + '\'' +
                ", country='" + country + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", region='" + region + '\'' +
                ", cloud='" + cloud + '\'' +
                '}';
    }
}
