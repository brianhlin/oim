package edu.iu.grid.oim.view.form;

import org.apache.commons.lang.StringEscapeUtils;

public class TextFormElement extends FormElementBase 
{	
	String value;
	
	public TextFormElement(String _name, String _label, String _value)
	{
		super(_name, _label);
		value = _value;
		if(value == null) {
			value = "";
		}
	}
	
	public String toHTML() {
		String out = "";
		out += "<span>"+label+":</span>";
		out += "<div>";
		out += "<input type=\"edit\" name=\""+name+"\" value=\""+
				StringEscapeUtils.escapeHtml(value)+"\"></input>";
		out += "</div>";
		return out;
	}
}
