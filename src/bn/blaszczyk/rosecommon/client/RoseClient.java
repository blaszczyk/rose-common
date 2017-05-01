package bn.blaszczyk.rosecommon.client;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.internal.StringMap;

import bn.blaszczyk.rosecommon.RoseException;
import bn.blaszczyk.rosecommon.dto.RoseDto;

public class RoseClient {
	
	public static final String CODING_CHARSET = "UTF-8";
	
	private static final Logger LOGGER = Logger.getLogger(RoseClient.class);

	private static final Gson GSON = new Gson();
	
	private final WebClient webClient;

	public RoseClient(final String url)
	{
		webClient = WebClient.create(url);
	}
	
	public RoseDto getDto(final String typeName, final int id) throws RoseException
	{
		final List<RoseDto> dtos = getDtos(typeName + "/" + id);
		if(dtos.size() != 1)
			throw new RoseException("error on GET@/" + typeName + "/" + id + "; found:" + dtos);
		return dtos.get(0);
	}
	
	public List<RoseDto> getDtos(final String subPath) throws RoseException
	{
		final String path = "/entity/" + subPath;
		try
		{
			LOGGER.debug("requesting GET@" + path);
			webClient.replacePath(path);
			webClient.resetQuery();
			final List<RoseDto> dtos = new ArrayList<>();
			final String encodedResponse = webClient.get(String.class);
			final String response = URLDecoder.decode(encodedResponse, CODING_CHARSET);
			final StringMap<?>[] stringMaps = GSON.fromJson(response, StringMap[].class);
			for(StringMap<?> stringMap : stringMaps)
				dtos.add(new RoseDto(stringMap));
			LOGGER.debug("decoded response message:\r\n" + response);
			return dtos;
		}
		catch (Exception e) 
		{
			throw RoseException.wrap(e, "Error on GET@" + path);
		}
	}

	public List<Integer> getIds(final String typeName) throws RoseException
	{
		final String path = "/entity/" + typeName + "/id";
		try
		{
			LOGGER.debug("requesting GET@" + path);
			webClient.replacePath(path);
			webClient.resetQuery();
			final String encodedResponse = webClient.get(String.class);
			final String response = URLDecoder.decode(encodedResponse, CODING_CHARSET);
			final String[] ids = GSON.fromJson(response, String[].class);
			LOGGER.debug("decoded response message:\r\n" + response);
			return Arrays.stream(ids)
						.map(String::trim)
						.map(Integer::parseInt)
						.collect(Collectors.toList());
		}
		catch (Exception e) 
		{
			throw RoseException.wrap(e, "Error on GET@" + path);
		}
	}

	public List<RoseDto> getDtos(final String typeName, final List<Integer> entityIds) throws RoseException
	{
		if(entityIds.isEmpty())
			return Collections.emptyList();
		return getDtos(typeName + "/" + commaSeparated(entityIds));
	}

	public int getCount(final String typeName) throws RoseException
	{
		final String path = "/entity/" + typeName + "/count";
		try
		{
			LOGGER.debug("requesting GET@/entity/" + typeName + "/count");
			webClient.replacePath(path);
			webClient.resetQuery();
			final String encodedResponse = webClient.get(String.class);
			final String response = URLDecoder.decode(encodedResponse, CODING_CHARSET);
			LOGGER.debug("decoded response message:\r\n" + response);
			return Integer.parseInt(response.trim());
		}
		catch (Exception e) 
		{
			throw RoseException.wrap(e, "Errpr on GET@" + path);
		}
	}

	public RoseDto postDto(final RoseDto dto) throws RoseException
	{
		final String path = pathForType(dto);
		try
		{
			LOGGER.debug("requesting POST@" + path);
			webClient.replacePath(path);
			webClient.resetQuery();
			final String request = GSON.toJson(dto);
			LOGGER.debug("decoded request message:\r\n" + request);
			final String encodedRequest = URLEncoder.encode(request, CODING_CHARSET);
			final String encodedResponse = webClient.post(encodedRequest,String.class);
			final String response = URLDecoder.decode(encodedResponse, CODING_CHARSET);
			LOGGER.debug("decoded response message:\r\n" + response);
			final StringMap<?> stringMap = GSON.fromJson(response, StringMap.class);
			return new RoseDto(stringMap);
		}
		catch(Exception e)
		{
			throw RoseException.wrap(e, "error on POST@" + path);
		}
	}

	public void putDto(final RoseDto dto) throws RoseException
	{
		final String path = pathFor(dto);
		try
		{
			LOGGER.debug("requesting PUT@" + path);
			webClient.replacePath(path);
			webClient.resetQuery();
			final String request = GSON.toJson(dto);
			LOGGER.debug("decoded request message:\r\n" + request);
			final String encodedRequest = URLEncoder.encode(request, CODING_CHARSET);
			webClient.put(encodedRequest);
		}
		catch (Exception e) 
		{
			throw RoseException.wrap(e, "error on PUT@" + path);
		}
	}

	public void deleteByID(final String typeName, final int id) throws RoseException
	{
		final String path = "/entity/" + typeName + "/" + id;

		try
		{
			LOGGER.debug("requesting DELETE@" + path);
			webClient.replacePath(path);
			webClient.resetQuery();
			webClient.delete();
		}
		catch (Exception e) 
		{
			throw RoseException.wrap(e, "error on DELETE@" + path);
		}
	}
	
	public Map<String,String> getServerStatus() throws RoseException
	{
		try
		{
			webClient.replacePath("/server/status");
			webClient.resetQuery();
			final String encodedResponse = webClient.get(String.class);
			final String response = URLDecoder.decode(encodedResponse, CODING_CHARSET);
			final StringMap<?> status = GSON.fromJson(response, StringMap.class);
			return status.entrySet().stream().
				collect(Collectors.toMap(e -> e.getKey(), e -> String.valueOf(e.getValue())));
		}
		catch (Exception e) 
		{
			throw RoseException.wrap(e, "error on GET@/server/status");
		}
	}
	
	public void close()
	{
		webClient.close();
	}
	
	private String pathForType(final RoseDto dto) throws RoseException
	{
		final Class<?> type = dto.getType();
		if(type == null)
			throw new RoseException("missing type in " + dto);
		return "/entity/" + type.getSimpleName().toLowerCase();
	}
	
	private String pathFor(final RoseDto dto) throws RoseException
	{
		final int id = dto.getId();
		if(id < 0)
			throw new RoseException("invalie id " + id);
		return pathForType(dto) + "/" + id;
	}
	
	private static String commaSeparated(final List<?> list)
	{
		boolean first = true;
		final StringBuilder sb = new StringBuilder();
		for(final Object o : list)
		{
			if(first)
				first = false;
			else
				sb.append(",");
			sb.append(o);
		}
		return sb.toString();
	}

}
