package com.att.aro.core.packetreader.model.pcapng;


import com.google.gson.annotations.SerializedName;
import lombok.Data;


@Data
public class Source {
	@SerializedName("layers")
	private Layers layers;
}
