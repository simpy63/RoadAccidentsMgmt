package com.epam.processor;

import com.epam.data.RoadAccident;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import java.util.*;
import java.util.stream.Collectors;

/**
 * This is to be completed by mentees
 */
public class DataProcessor {

    private final List<RoadAccident> roadAccidentList;

    public DataProcessor(List<RoadAccident> roadAccidentList){
        this.roadAccidentList = roadAccidentList;
    }


//    First try to solve task using java 7 style for processing collections

    /**
     * Return road accident with matching index
     * @param index
     * @return
     */
    public RoadAccident getAccidentByIndex7(String index){
        for(RoadAccident roadAccident : roadAccidentList){
            if(roadAccident.getAccidentId().equals(index)) return roadAccident;
        }
        return null;
    }


    /**
     * filter list by longtitude and latitude values, including boundaries
     * @param minLongitude
     * @param maxLongitude
     * @param minLatitude
     * @param maxLatitude
     * @return
     */
    public Collection<RoadAccident> getAccidentsByLocation7(float minLongitude, float maxLongitude, float minLatitude, float maxLatitude){
        Collection<RoadAccident> matchedAccidents = new ArrayList<>();
        float longitude, latitude;
        for(RoadAccident roadAccident: this.roadAccidentList){
            longitude = roadAccident.getLongitude();
            latitude = roadAccident.getLatitude();
            if(  Float.compare(longitude,minLongitude) >= 0 && Float.compare(longitude, maxLongitude) <= 0
              && Float.compare(latitude, minLatitude)  >= 0 && Float.compare(latitude, maxLatitude)   <= 0){
                matchedAccidents.add(roadAccident);
            }
        }
        return matchedAccidents;
    }

    /**
     * count incidents by road surface conditions
     * ex:
     * wet -> 2
     * dry -> 5
     * @return
     */
    public Map<String, Long> getCountByRoadSurfaceCondition7(){
        Map<String, Long> roadSurfaceStats = new HashMap<>();
        String roadSurfaceCondition;
        for(RoadAccident roadAccident: roadAccidentList){
            roadSurfaceCondition = roadAccident.getRoadSurfaceConditions();
            Long conditionCount = roadSurfaceStats.get(roadSurfaceCondition);
            if(conditionCount == null) conditionCount = 0l;
            roadSurfaceStats.put(roadSurfaceCondition,++conditionCount);

        }
        return roadSurfaceStats;
    }

    /**
     * find the weather conditions which caused the top 3 number of incidents
     * as example if there were 10 accidence in rain, 5 in snow, 6 in sunny and 1 in foggy, then your result list should contain {rain, sunny, snow} - top three in decreasing order
     * @return
     */
    public List<String> getTopThreeWeatherCondition7(){
        Map<String, Long> accidentStats = new HashMap<>();
        Map<String, Long> sortedAccidentStats = new TreeMap<>(new Comparator<String>(){
            @Override
            public int compare(String o1, String o2) {
                //TODO:: Make more generic.
                return accidentStats.get(o2).compareTo(accidentStats.get(o1));
            }
        });

        String weatherCondition;
        for(RoadAccident roadAccident: roadAccidentList){
            weatherCondition = roadAccident.getWeatherConditions();
            Long accidentCount = accidentStats.get(weatherCondition);
            if(accidentCount == null) accidentCount = 0l;
            accidentStats.put(weatherCondition,++accidentCount);

        }
        sortedAccidentStats.putAll(accidentStats);

        return new ArrayList<>(sortedAccidentStats.keySet()).subList(0, 3);
    }

    /**
     * return a multimap where key is a district authority and values are accident ids
     * ex:
     * authority1 -> id1, id2, id3
     * authority2 -> id4, id5
     * @return
     */
    public Multimap<String, String> getAccidentIdsGroupedByAuthority7(){
        Multimap<String, String> multimap = ArrayListMultimap.create();
        for(RoadAccident roadAccident: roadAccidentList){
            multimap.put(roadAccident.getDistrictAuthority(), roadAccident.getAccidentId());
        }
        return multimap;
    }


    // Now let's do same tasks but now with streaming api



    public RoadAccident getAccidentByIndex(String index){

        return roadAccidentList
               .stream()
               .filter(accident -> accident.getAccidentId().equals(index))
               .findFirst()
               .orElse(null);
    }


    /**
     * filter list by longtitude and latitude fields
     * @param minLongitude
     * @param maxLongitude
     * @param minLatitude
     * @param maxLatitude
     * @return
     */
    public Collection<RoadAccident> getAccidentsByLocation(float minLongitude, float maxLongitude, float minLatitude, float maxLatitude){
        return roadAccidentList
               .parallelStream()
               .filter(roadAccident -> (  roadAccident.getLongitude() >= minLongitude
                                        && roadAccident.getLongitude() <= maxLongitude
                                        && roadAccident.getLatitude()  >= minLatitude
                                        && roadAccident.getLatitude()  <= maxLatitude))
               .collect(Collectors.toList());
    }

    /**
     * find the weather conditions which caused max number of incidents
     * @return
     */
    public List<String> getTopThreeWeatherCondition(){
        return roadAccidentList
               .stream()
               .collect(Collectors.groupingBy(RoadAccident::getWeatherConditions, Collectors.counting()))
               .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .limit(3)
                .map(roadAccident  -> roadAccident.getKey()).collect(Collectors.toList());
    }

    /**
     * count incidents by road surface conditions
     * @return
     */
    public Map<String, Long> getCountByRoadSurfaceCondition(){
        return roadAccidentList
               .stream()
               .collect(Collectors.groupingBy(RoadAccident::getRoadSurfaceConditions, Collectors.counting()));
    }

    /**
     * To match streaming operations result, return type is a java collection instead of multimap
     * @return
     */
    public Map<String, List<String>> getAccidentIdsGroupedByAuthority(){
        return roadAccidentList
                .stream()
                .collect(Collectors.groupingBy(
                                     RoadAccident::getDistrictAuthority,
                                     Collectors.mapping(
                                              RoadAccident::getAccidentId,
                                              Collectors.toList()
                                              )
                                    )
                );
    }

}
