package me.sureshs.cowinalert.model.result;

import java.util.Comparator;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Result implements Comparable<Result> {

	private String name;

	// private String address;

	private String fee_type;

	private String date;

	private Integer available_capacity;

	private String vaccine;

	@Override
	public String toString() {
		return name + " | " + date + " | " + available_capacity + " | " + vaccine + " | " + fee_type;
	}

	@Override
	public int compareTo(Result r) {
		return Comparator.comparing(Result::getName).thenComparing(Result::getDate).compare(this, r);
	}

}
