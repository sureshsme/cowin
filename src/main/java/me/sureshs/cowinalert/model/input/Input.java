package me.sureshs.cowinalert.model.input;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Input {

	private String chat_id;
	
	private String name;
	
	private String district_id;
	
	private Integer dose;
	
	private Integer min_age_limit;
	
	private String vaccine;

	@Override
	public String toString() {
		return chat_id + " | " + name + " | " + district_id + " | " + dose + " | " + min_age_limit + " | " + vaccine;
	}
	
	//private Character active;
	
	
}
