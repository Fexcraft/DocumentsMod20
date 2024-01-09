package net.fexcraft.mod.documents;

import net.minecraft.resources.ResourceLocation;
import java.util.*;

public class ExternalTextures {

	private static final Map<String, ResourceLocation> MAP = new HashMap<String, ResourceLocation>();
	private static final HashSet<String> KEY = new HashSet<>();
	private static boolean added;
	static{ KEY.add("documents"); }

	public static ResourceLocation get(String url){
		if(MAP.containsKey(url)) return MAP.get(url);
		ResourceLocation texture = new ResourceLocation("documents", url.replaceAll("[^a-z0-9_.-]", ""));
		MAP.put(url, texture);
		//TODO
		return texture;
	}

}
