package com.webif.divex;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import edu.iu.grid.oim.servlet.AdminServlet;

public abstract class DivEx {
    static Logger log = Logger.getLogger(DivEx.class);  
    
	private String nodeid;
	private Boolean needupdate = false;
	private String js = "";
	private DivEx parent;
	
	public DivEx(DivEx _parent) {
		parent = _parent;
		nodeid = DivExRoot.getNewNodeID();
		if(_parent != null) {
			_parent.add(this);
		}
	}
	public DivExRoot getRoot()
	{
		if(parent == null) {
			return (DivExRoot)this;
		}
		return parent.getRoot();
	}
	
	protected ArrayList<DivEx> childnodes = new ArrayList<DivEx>();
	public void add(DivEx child)
	{
		childnodes.add(child);
	}
	public void remove(DivEx child)
	{
		childnodes.remove(child);
	}
	 
	public void alert(String msg)
	{
		js += "alert(\""+msg+"\");";
	}
	public void js(String _js)
	{
		js += _js;
	}
	public void redirect(String url) {
		//if we emit redirect, we don't want to emit anything else.. just jump!
		getRoot().redirect(url);
	}
	
	//set container(jquery selector) to null if you want to scroll the whole page.
	public void scrollToShow(String container) {
		js += "var targetOffset = $(\"#"+nodeid+"\").offset().top;";
		if(container == null) {
			js += "$('html,body').animate({scrollTop: targetOffset}, 500);";
		} else {
			js += "targetOffset -= $('"+container+"').offset().top;";
			js += "$('"+container+"').scrollTop(targetOffset);";
		}
	}

	private ArrayList<EventListener> event_listeners = new ArrayList<EventListener>();
	
	public String getNodeID() { return nodeid; }

	//DivEx calls this on *root* upon completion of event handler
	protected String outputUpdatecode()
	{
		//start with user specified js code
		String code = js;
		js = "";
		
		//find child nodes who needs update
		if(needupdate) {
			code += "divex_replace($(\"#"+nodeid+"\"), \"divex?action=load&nodeid="+nodeid+"\");";
			//I don't need to update any of my child - parent will redraw all of it.
			setNeedupdate(false);
		} else {
			//see if any of my children needs update
			for(DivEx d : childnodes) {
				code += d.outputUpdatecode();
			}
		}
		
		return code;
	}
	
	protected void flushJS(PrintWriter out)
	{
		out.write(js);
		js = "";
		for(DivEx d : childnodes) {
			d.flushJS(out);
		}
	}
	
	//recursively set mine and my children's needupdate flag
	public void setNeedupdate(Boolean b)
	{
		needupdate = b;
		for(DivEx node : childnodes) {
			node.setNeedupdate(b);
		}
	}
	
	//recursively do search
	public DivEx findNode(String _nodeid)
	{
		if(nodeid.compareTo(_nodeid) == 0) return this;
		for(DivEx child : childnodes) {
			DivEx node = child.findNode(_nodeid);
			if(node != null) return node;
		}
		return null;
	}
	
	abstract public void render(PrintWriter out);
	public void addEventListener(EventListener listener)
	{
		event_listeners.add(listener);
	}
		
	public void doGet(DivExRoot root, HttpServletRequest request, HttpServletResponse response) throws IOException
	{	
		String action = request.getParameter("action");
		log.debug("DivEx Action:" + action);
		
		if(action.compareTo("load") == 0) {
			PrintWriter writer = response.getWriter();
			response.setContentType("text/html");
			render(writer);
			
			//additionally, output any javascript
			writer.write("<script type=\"text/javascript\">");
			writer.print(root.outputUpdatecode());
			writer.write("</script>");
			
		} else if(action.compareTo("request") == 0) {
			//it could be any content type - let handler decide
			this.onRequest(request, response);
		} else {
			String value = request.getParameter("value");
			Event e = new Event(action, value);

			//handle my event handler
			onEvent(e);
			notifyListener(e);

			//emit all requested update code
			PrintWriter writer = response.getWriter();
			response.setContentType("text/javascript");
			writer.print(root.outputUpdatecode());
		}
	}
	protected void notifyListener(Event e)
	{
		//notify event listener
		for(EventListener listener : event_listeners) {
			listener.handleEvent(e);
		}
	}
	
	//events are things like click, drag, change.. you are responsible for updating 
	//the internal state of the target div, and framework will call outputUpdatecode()
	//to emit re-load request which will then re-render the divs that are changed.
	//Override this to handle local events (for remote events, use listener)
	abstract protected void onEvent(Event e);
	
	//request are things like outputting XML or JSON back to browser without changing
	//any internal state. it's like load but it doesn't return html necessary. it could
	//be XML, JSON, Image, etc.. The framework will not emit any update code
	protected void onRequest(HttpServletRequest request, HttpServletResponse response) {}
	
	//only set needupdate on myself - since load will redraw all its children
	//once drawing is done, needudpate = false will be performec recursively
	public void redraw() {
		needupdate = true;
	}
	/*
	public static String encodeHTML(String s)
	{
	    StringBuffer out = new StringBuffer();
	    for(int i=0; i<s.length(); i++)
	    {
	        char c = s.charAt(i);
	        if(c > 127 || c=='"' || c=='\'' || c=='<' || c=='>')
	        {
	           out.append("&#"+(int)c+";");
	        }
	        else
	        {
	            out.append(c);
	        }
	    }
	    return out.toString();
	}
	*/
}
