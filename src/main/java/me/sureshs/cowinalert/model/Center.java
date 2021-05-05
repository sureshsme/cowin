package me.sureshs.cowinalert.model;

import java.util.List;

import lombok.Data;

@Data
public class Center {

	private Long center_id;
	
	private String name;
	
	private String address;
	
	private String state_name;
	
	private String district_name;
	
	private String block_name;
	
	private Long pincode;
	
	private String fee_type;
	
	private List<Session> sessions;
	
}
