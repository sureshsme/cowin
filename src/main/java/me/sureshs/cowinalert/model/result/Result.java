package me.sureshs.cowinalert.model.result;

import java.util.Comparator;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Result implements Comparable<Result> {

	private String name;

	private String fee_type;

	private String date;

	private Integer available_capacity;

	private String vaccine;

	private Integer min_age_limit;

	private Integer available_capacity_dose1;
	
	private Integer available_capacity_dose2;

	@Override
	public String toString() {
		return name + " | " + date + " | age: " + min_age_limit + " | " + vaccine + " | D1: " + available_capacity_dose1 + " | D2:" + available_capacity_dose2;
	}

	@Override
	public int compareTo(Result r) {
		return Comparator.comparing(Result::getName).thenComparing(Result::getDate).compare(this, r);
	}

}
