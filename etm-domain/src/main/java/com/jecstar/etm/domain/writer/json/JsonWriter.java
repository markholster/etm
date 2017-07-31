package com.jecstar.etm.domain.writer.json;

import java.net.InetAddress;
import java.util.Date;
import java.util.Set;
import java.util.stream.Collectors;

public class JsonWriter {

	private static final char[] HEX_CHARS = new char[] {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
	private static final int NO_ESCAPE = 0;
	private static final int STANDARD_ESCAPE = -1;
    private static final int[] ESCAPE_TABLE;
    static {
        int[] table = new int[128];
        for (int i = 0; i < 32; ++i) {
            table[i] = STANDARD_ESCAPE;
        }
        table['"'] = '"';
        table['\\'] = '\\';
        table[0x08] = 'b';
        table[0x09] = 't';
        table[0x0C] = 'f';
        table[0x0A] = 'n';
        table[0x0D] = 'r';
        ESCAPE_TABLE = table;
    }	
    
	public boolean addStringElementToJsonBuffer(String elementName, String elementValue, StringBuilder buffer, boolean firstElement) {
		return addStringElementToJsonBuffer(elementName, elementValue, false, buffer, firstElement);
	}
	
	public boolean addStringElementToJsonBuffer(String elementName, String elementValue, boolean writeWhenNull, StringBuilder buffer, boolean firstElement) {
		if (elementValue == null && !writeWhenNull) {
			return false;
		}
		if (!firstElement) {
			buffer.append(", ");
		}
		buffer.append(escapeToJson(elementName, true)).append(": ").append(escapeToJson(elementValue, true));
		return true;
	}

	public boolean addLongElementToJsonBuffer(String elementName, Long elementValue, StringBuilder buffer, boolean firstElement) {
		if (elementValue == null) {
			return false;
		}
		if (!firstElement) {
			buffer.append(", ");
		}
		buffer.append(escapeToJson(elementName, true)).append(": ").append(elementValue);
		return true;
	}
	
	public boolean addDoubleElementToJsonBuffer(String elementName, Double elementValue, StringBuilder buffer, boolean firstElement) {
		if (elementValue == null || elementValue.isNaN()) {
			return false;
		}
		if (!firstElement) {
			buffer.append(", ");
		}
		buffer.append(escapeToJson(elementName, true)).append(": ").append(elementValue);
		return true;
	}
	
	public boolean addIntegerElementToJsonBuffer(String elementName, Integer elementValue, StringBuilder buffer, boolean firstElement) {
		if (elementValue == null) {
			return false;
		}
		if (!firstElement) {
			buffer.append(", ");
		}
		buffer.append(escapeToJson(elementName, true)).append(": ").append(elementValue);
		return true;
	}
	
	public boolean addBooleanElementToJsonBuffer(String elementName, Boolean elementValue, StringBuilder buffer, boolean firstElement) {
		if (elementValue == null) {
			return false;
		}
		if (!firstElement) {
			buffer.append(", ");
		}
		buffer.append(escapeToJson(elementName, true)).append(": ").append(elementValue);
		return true;
	}
	
	public boolean addInetAddressElementToJsonBuffer(String hostAddressTag, String hostNameTag, InetAddress elementValue, StringBuilder buffer, boolean firstElement) {
		if (elementValue == null) {
			return false;
		}
		if (!firstElement) {
			buffer.append(", ");
		}
		buffer.append(escapeToJson(hostAddressTag, true)).append(": ").append(escapeToJson(elementValue.getHostAddress(), true));
		buffer.append(", ").append(escapeToJson(hostNameTag, true)).append(": ").append(escapeToJson(elementValue.getHostName(), true));
		return true;
	}
	
	public boolean addSetElementToJsonBuffer(String elementName, Set<String> elementValues, StringBuilder buffer, boolean firstElement) {
		if (elementValues.size() < 1) {
			return false;
		}
		if (!firstElement) {
			buffer.append(", ");
		}
		buffer.append(escapeToJson(elementName, true)).append(": [");
		buffer.append(elementValues.stream()
				.map(c -> escapeToJson(c, true))
				.sorted()
				.collect(Collectors.joining(", ")));
		buffer.append("]");
		return true;
	}
	
	public boolean addSetElementToJsonBuffer(String elementName, Set<String> elementValues, boolean writeWhenEmpty, StringBuilder buffer, boolean firstElement) {
		if (elementValues.size() < 1 &&!writeWhenEmpty) {
			return false;
		}
		if (!firstElement) {
			buffer.append(", ");
		}
		buffer.append(escapeToJson(elementName, true)).append(": [");
		buffer.append(elementValues.stream()
				.map(c -> escapeToJson(c, true))
				.sorted()
				.collect(Collectors.joining(", ")));
		buffer.append("]");
		return true;
	}
	
	public String escapeToJson(String value, boolean quote) {
		if (value == null) {
			return "null";
		}
		int maxLength = value.length() * 6;
		if (quote) {
			maxLength += 2;
		}
		char[] outputBuffer = new char[maxLength];
        final int escLen = ESCAPE_TABLE.length;
        int outputPointer = 0;
        if (quote) {
        	outputBuffer[outputPointer++] = '"';
        }
        conversion_loop:
        for (int i=0; i < value.length(); i++) {
            while (true) {
                char c = value.charAt(i);
                if (c < escLen && ESCAPE_TABLE[c] != NO_ESCAPE) {
                    break;
                }
                outputBuffer[outputPointer++] = c;
                if (++i >= value.length()) {
                    break conversion_loop;
                }
            }
            char c = value.charAt(i);
            outputPointer = appendCharacterEscape(outputBuffer, outputPointer, c, ESCAPE_TABLE[c]);
        }		
        if (quote) {
        	outputBuffer[outputPointer++] = '"';
        }
        char[] result = new char[outputPointer];
        System.arraycopy(outputBuffer, 0, result, 0, outputPointer);
        return new String(result);
	}

	private int appendCharacterEscape(char[] outputBuffer, int outputPointer, char ch, int escCode) {
        if (escCode > NO_ESCAPE) {
    		outputBuffer[outputPointer++] = '\\';
    		outputBuffer[outputPointer++] = (char) escCode;
            return outputPointer;
        }
	    if (escCode == STANDARD_ESCAPE) {
            outputBuffer[outputPointer++] = '\\';
            outputBuffer[outputPointer++] = 'u';
            // We know it's a control char, so only the last 2 chars are non-0
            if (ch > 0xFF) { // beyond 8 bytes
                int hi = (ch >> 8) & 0xFF;
                outputBuffer[outputPointer++] = HEX_CHARS[hi >> 4];
                outputBuffer[outputPointer++] = HEX_CHARS[hi & 0xF];
                ch &= 0xFF;
            } else {
            	outputBuffer[outputPointer++] = '0';
            	outputBuffer[outputPointer++] = '0';
            }
            outputBuffer[outputPointer++] = HEX_CHARS[ch >> 4];
            outputBuffer[outputPointer++] = HEX_CHARS[ch & 0xF];
            return outputPointer;
        }
	    outputBuffer[outputPointer++] = (char) escCode;
	    return outputPointer;
    }
	
	/**
	 * Escape an object to a json name/value pair. The value is always
	 * considered to be a String, but if the value happens to be a
	 * <code>Number</code>, <code>Boolean</code> or <code>Date</code> a second
	 * name value/pair is added to the response with the specific value.
	 * 
	 * @param name
	 *            The name part of the name/value pair.
	 * @param value
	 *            The value part of the name/value pair.
	 * @return The name/value escaped to json.
	 */
	public String escapeObjectToJsonNameValuePair(String name, Object value) {
		if (value == null) {
			return escapeToJson(name, true) + ": " + "null";
		} else if (value instanceof Number) {
			return escapeToJson(name, true) + ": \"" + value.toString() + "\", " + escapeToJson(name + "_as_number", true) + ": " + value.toString();
		} else if (value instanceof Boolean) {
			return escapeToJson(name, true) + ": \"" + value.toString() + "\", " + escapeToJson(name + "_as_boolean", true) + ": " + value.toString();
		} else if (value instanceof Date) {
			return escapeToJson(name, true) + ": \"" + value.toString() + "\", " + escapeToJson(name + "_as_date", true) + ": " + ((Date)value).getTime();
		}
		return escapeToJson(name, true) + ": " + escapeToJson(value.toString(), true);		
	}

	
}
