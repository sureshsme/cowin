package me.sureshs.cowinalert.model;

import lombok.Data;

@Data
public class Session {

	private String date;
	
	private Integer available_capacity;
	
	private Integer min_age_limit;
	
	private String vaccine;
	
	private Integer available_capacity_dose1;
	
	private Integer available_capacity_dose2;
	
}
