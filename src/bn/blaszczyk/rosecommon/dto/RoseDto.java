package bn.blaszczyk.rosecommon.dto;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.google.gson.Gson;
import com.google.gson.internal.StringMap;

import bn.blaszczyk.rose.RoseException;
import bn.blaszczyk.rose.model.Readable;
import bn.blaszczyk.rose.model.Timestamped;
import bn.blaszczyk.rosecommon.tools.TypeManager;

public class RoseDto extends LinkedHashMap<String, String>{
	
	private static final String ID_KEY = "id";
	private static final String TYPE_KEY = "type";
	private static final String TIMESTAMP_KEY = "timestamp";

	private static final long serialVersionUID = 7851913867785528671L;
	
	public static final DateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy");
	public static final DecimalFormat BIG_DEC_FORMAT = (DecimalFormat) NumberFormat.getNumberInstance();
	static{
		BIG_DEC_FORMAT.setParseBigDecimal(true);
	}

	private static final Gson GSON = new Gson();	
	private static final Pattern ID_PATTERN = Pattern.compile("^\\-?[0-9]*$");
	
	public RoseDto(final StringMap<?> stringMap) throws RoseException
	{
		for(final Map.Entry<String, ?> entry : stringMap.entrySet())
		{
			checkEntry(entry);
			put(entry.getKey(), String.valueOf(entry.getValue()));
		}
	}

	public RoseDto(final Map<String, String[]> parameterMap) throws RoseException
	{
		for(final Map.Entry<String, String[]> entry : parameterMap.entrySet())
		{
			checkArrayEntry(entry);
			put(entry.getKey(), entry.getValue()[0]);
		}
	}
	
	public RoseDto(final Readable entity)
	{
		put(TYPE_KEY,entity.getEntityName());
		if(entity.getId() >= 0)
			put(ID_KEY, String.valueOf(entity.getId()));
		if(entity instanceof Timestamped)
			put(TIMESTAMP_KEY, Long.toString( ((Timestamped)entity).getTimestamp().getTime() ));
		for(int i = 0; i < entity.getFieldCount(); i++)
		{
			String stringValue = null;
			final Object field = entity.getFieldValue(i);
			if(field instanceof Date)
				stringValue = DATE_FORMAT.format((Date)field);
			else if(field != null)
				stringValue = field.toString();
			final String name = entity.getFieldName(i).toLowerCase();
			put(name,stringValue);
		}
		for(int i = 0; i < entity.getEntityCount(); i++)
		{
			final String name = entity.getEntityName(i).toLowerCase();
			if(entity.getRelationType(i).isSecondMany())
			{
				final List<Integer> ids = new ArrayList<>();
				for(final Readable subEntity : entity.getEntityValueMany(i))
					ids.add(subEntity.getId());
				ids.sort((i1,i2) -> Integer.compare(i1, i2));
				put(name,GSON.toJson(ids));
			}
			else
			{
				Readable subEntity = entity.getEntityValueOne(i);
				if(subEntity != null)
					put(name,String.valueOf(subEntity.getId()));
			}
		}
	}

	public int getId()
	{
		return Integer.parseInt(get(ID_KEY));
	}
	
	public Class<? extends Readable> getType()
	{
		return TypeManager.getClass(get(TYPE_KEY));
	}
	
	public boolean hasTimestamp()
	{
		return containsKey(TIMESTAMP_KEY);
	}
	
	public Date getTimestamp()
	{
		final long timestamp = Long.parseLong(get(TIMESTAMP_KEY));
		return new Date(timestamp);
	}
	
	public String getFieldValue(final String fieldName)
	{
		return get(fieldName.toLowerCase());
	}
	
	public Integer getEntityId(final String fieldName)
	{
		final String id = get(fieldName.toLowerCase());
		if(id == null)
			return -1;
		return Integer.parseInt(id);
	}
	
	public List<Integer> getEntityIds(final String fieldName)
	{
		final String idsString = get(fieldName.toLowerCase());
		if(idsString == null)
			return Collections.emptyList();
		final Integer[] ids = GSON.fromJson(idsString, Integer[].class);
		return Arrays.asList(ids);
	}
	
	private void checkArrayEntry(final Map.Entry<String, String[]> entry) throws RoseException
	{
		final String[] value = entry.getValue();
		if(value.length != 1)
			throw new RoseException("array value " + entry.getKey() + "=" + value + " has no unique element");
		checkEntry(entry.getKey(), value[0]);
	}

	private void checkEntry(final Map.Entry<String, ?> entry) throws RoseException
	{
		checkEntry(entry.getKey(), String.valueOf(entry.getValue()));
	}
	
	private void checkEntry(final String key, final String value) throws RoseException
	{
		if(key.equals(ID_KEY))
		{
			if(! ID_PATTERN.matcher(String.valueOf(value)).matches())
				throw new RoseException("not matching an id value: '" + value + "'");
		}
		else if(key.equals(TYPE_KEY))
		{
			if( TypeManager.getClass(value) == null)
				throw new RoseException("unknown type '" + value + "'");
		}
		else if(key.equals(TIMESTAMP_KEY))
		{
			if(! ID_PATTERN.matcher(String.valueOf(value)).matches())
				throw new RoseException("not matching an id value: '" + value + "'");
		}
	}
	
}
