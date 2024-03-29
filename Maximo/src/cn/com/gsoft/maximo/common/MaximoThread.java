/* ==================================================================   
 * Created [2009-7-22 下午01:58:03] by Wan Fei
 * ==================================================================  
 * MaximoThread.java
 * ================================================================== 
 * Copyright (c) Gsoft S&T Co.ltd HangZhou, 2008-2009 
 * ================================================================== 
 * 杭州中科天翔科技有限公司拥有该文件的使用、复制、修改和分发的许可权
 * 如果你想得到更多信息，请访问 <http://www.g-soft.com.cn>
 *
 * Gsoft S&T Co.ltd HangZhou owns permission to use, copy, modify and 
 * distribute this documentation.
 * For more information on MaximoThread.java, please 
 * see <http://www.g-soft.com.cn>.  
 * ================================================================== 
 */

package cn.com.gsoft.maximo.common;

import java.io.*;
import org.xml.sax.*;
import org.apache.xerces.parsers.*;
import lotus.domino.*;

/**
 * MaximoThread.java
 * @author Wan Fei
 * 遍历函件库数据的线程类
 */
public class MaximoThread extends Thread{

	private long TIMER_INTEVAL = 30000;		//遍历间隔时间，单位毫秒
	private String server = "127.0.0.1";	//Domino函件数据库地址
    private String user = "admin";			//DIIOP登录用户名称
    private String pass = "password";		//登录用户密码
    private Session session;
	
	public MaximoThread(){
		this.server = Config.getAttribute("Mailip");	//函件库所在服务器
		this.user = Config.getAttribute("Username");
		this.pass = Config.getAttribute("Password");
		this.TIMER_INTEVAL = Long.parseLong(Config.getAttribute("Timer_Inteval"));
		this.session = getNotesSession();
	}
	
	public MaximoThread(MaximoThread mt){
		this.server = mt.server;
		this.user = mt.user;
		this.pass = mt.pass;
		this.TIMER_INTEVAL = mt.TIMER_INTEVAL;
		this.session = mt.session;
	}
	
	public Session getNotesSession(){
		if(this.session!=null)
			return this.session;
		else{
			try{
				this.session = NotesFactory.createSession(this.server+":63148", this.user, this.pass);
				Logger.log("Maximo接口登录服务器 " + this.server + " 成功！");				
	        }catch(NotesException e){
	            Logger.log("Maximo接口登录服务器 " + this.server + " 失败！");
	            Logger.log(e.toString());
	        }
	        return this.session;
		}
	}
	
	public void getFirstDocumentToTarget(){
		try{
			Session session = getNotesSession();
			Database db = session.getDatabase("", "mail/maximo.nsf");
			if(db!=null){
				View vw = db.getView("NotSend");
				Document doc = vw.getFirstDocument();
				if(doc!=null){
					session.setConvertMIME(true);
					String xml = doc.getItemValueString("Body");
					xml = xml.replaceAll("\r\n", "");					
					xml = xml.toLowerCase();
					if(xml.indexOf("incident")!=-1){
						xml = xml.substring(0, xml.indexOf("</incident>")+11);
						createDominoOaDoc(doc, xml);
					}else if(xml.indexOf("worklogs")!=-1){
						xml = xml.substring(0, xml.indexOf("</worklogs>")+11);
						createMaximoLogDoc(doc, xml);
					}					
					db.recycle();
				}
			}
			else{
				Logger.log("Maximo函件数据库未配置！");
				Appsetting.setRunninng(false);
			}
		}catch(NotesException e){
			Logger.log(e.toString());
		}	
	}
	
	public void createDominoOaDoc(Document doc, String xml){
		org.w3c.dom.Document xmldoc = null;
		StringReader sr = new StringReader(xml);
		InputSource iSrc = new InputSource(sr);
		DOMParser parser = new DOMParser();
		try{
			parser.parse(iSrc);
			xmldoc = parser.getDocument();
			MaximoDoc mdoc = new MaximoDoc(xmldoc);
			TargetServer ts = new TargetServer();
			ts.setExtsiteid(mdoc.getExtsiteid());
			ts.setExtorgid(mdoc.getExtorgid());
			ts = ts.getServerNameByKey(session);
			DominoOaDoc oadoc = new DominoOaDoc(session, mdoc, ts, doc);						
			String backinfo = oadoc.createTargetServerDoc();
			if(!backinfo.equals("")){							
				Logger.log("工单："+mdoc.getTicketid()+" 到 "+ts.getExtsiteid()+" "+ts.getExtorgid()+" 发送失败！！！");
				Logger.log(backinfo);
				setSendFlag(doc, "-1", mdoc.getTicketid(), ts.getExtsiteid(), ts.getExtorgid(), backinfo);
			}else{
				setSendFlag(doc, "1", mdoc.getTicketid(), ts.getExtsiteid(), ts.getExtorgid(), null);
				Logger.log("工单："+mdoc.getTicketid()+" 到 "+ts.getExtsiteid()+" "+ts.getExtorgid()+" 发送成功！");
			}
		}
		catch(IOException e){
			Logger.log(e.toString());
			setSendFlag(doc, "-1", "", "", "", e.toString());
		}
		catch(SAXException e){
			Logger.log(e.toString());
			setSendFlag(doc, "-1", "", "", "", e.toString());
		}
	}
	
	public void createMaximoLogDoc(Document doc, String xml){
		org.w3c.dom.Document xmldoc = null;
		StringReader sr = new StringReader(xml);
		InputSource iSrc = new InputSource(sr);
		DOMParser parser = new DOMParser();
		try{
			parser.parse(iSrc);
			xmldoc = parser.getDocument();
			ReturnLogDoc logdoc = new ReturnLogDoc(xmldoc, xml);
			String backinfo = logdoc.returnLogs();
			if(!backinfo.equals("")){							
				Logger.log("工单日志："+logdoc.getTicketid()+" 从 "+logdoc.getSiteid()+" "+logdoc.getOrgid()+" 回写失败！！！");
				Logger.log(backinfo);
				setSendFlag(doc, "-1", logdoc.getTicketid(), logdoc.getSiteid(), logdoc.getOrgid(), backinfo);
			}else{
				setSendFlag(doc, "1", logdoc.getTicketid(), logdoc.getSiteid(), logdoc.getOrgid(), null);
				Logger.log("工单日志："+logdoc.getTicketid()+" 从 "+logdoc.getSiteid()+" "+logdoc.getOrgid()+" 回写成功！");
			}
		}
		catch(IOException e){
			Logger.log(e.toString());
			setSendFlag(doc, "-1",  "", "", "", e.toString());
		}
		catch(SAXException e){
			Logger.log(e.toString());
			setSendFlag(doc, "-1",  "", "", "", e.toString());
		}
	}
	
	public void setSendFlag(Document doc, String flag, String ticketid, String siteid, String orgid, String error){
		try{
			doc.replaceItemValue("SENDFLAG", flag);
			if(!ticketid.equals("")){
				doc.replaceItemValue("ticketid", ticketid);				
			}
			if(!siteid.equals("")){
				doc.replaceItemValue("siteid", siteid);
			}
			if(!orgid.equals("")){
				doc.replaceItemValue("orgid", orgid);
			}
			if(error!=null){
				doc.replaceItemValue("error", error);
			}
			doc.save();
		}
		catch(NotesException e){
			Logger.log(e.toString());
		}		
	}
	
	public static void main(String[] args){
		Logger.ini();
		MaximoThread a = new MaximoThread();
		a.getFirstDocumentToTarget();
		//a.start();
	}
	
	public void run(){
        try{
            while(Appsetting.getRunning()){
            	MaximoThread mt = new MaximoThread(this);
            	mt.getFirstDocumentToTarget();
                Thread.sleep(this.TIMER_INTEVAL);
            }
        }catch(Exception e){
        		
        }
    }
}
