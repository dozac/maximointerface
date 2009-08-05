/* ==================================================================   
 * Created [2009-7-23 下午04:44:43] by Wan Fei
 * ==================================================================  
 * DominoOaDoc.java
 * ================================================================== 
 * Copyright (c) Gsoft S&T Co.ltd HangZhou, 2008-2009 
 * ================================================================== 
 * 杭州中科天翔科技有限公司拥有该文件的使用、复制、修改和分发的许可权
 * 如果你想得到更多信息，请访问 <http://www.g-soft.com.cn>
 *
 * Gsoft S&T Co.ltd HangZhou owns permission to use, copy, modify and 
 * distribute this documentation.
 * For more information on DominoOaDoc.java, please 
 * see <http://www.g-soft.com.cn>.  
 * ================================================================== 
 */

package cn.com.gsoft.maximo.common;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

import lotus.domino.*;

/**
 * DominoOaDoc.java
 * @author Wan Fei
 *
 */
public class DominoOaDoc {

	private Session session;	//访问地市Domino会话，可由MaximoThread获取传入
	private MaximoDoc mdoc;		//分析XML后的Maximo邮件传过来的类实例
	private TargetServer ts;	//目标服务器配置信息文档
	private Document olddoc;	//MaximoThread获得的需要解析的函件（用于更方便地获取附件）
	
	public DominoOaDoc(Session session, MaximoDoc mdoc, TargetServer ts, Document olddoc){
		this.session = session;
		this.mdoc = mdoc;
		this.ts = ts;
		this.olddoc = olddoc;
	}
	
	public String createTargetServerDoc(){
		Database db;
		Document olddoc = this.olddoc;
		String backinfo = "";
		try{
			db = session.getDatabase(ts.getServername(), ts.getDbname());	//目标服务器上的FOA应用库
			Document doc = db.createDocument();	//目标应用库新建流转文档
			doc = olddoc.copyToDatabase(db);	//将附件拷贝到新建文档

			//获得并设置附件，把其余域删除
			Vector items = doc.getItems();
			for (int j=0; j<items.size(); j++) {
				Item item = (Item)items.elementAt(j);
				if(!item.getName().equals("$FILE")){
					item.remove();
				}
			}
			
			String attLogInfo = mdoc.getCreateby()+"|"+getTime()+"|U|";
			Vector v = session.evaluate("@AttachmentNames", doc);
			Item AttLog = doc.replaceItemValue("AttLog", null);	//用于匹配FOA3.0的附件操作
			for(int i=0;i<v.size();i++){
				String attname = (String)v.get(i);
				if(!attname.equals(""))
					AttLog.appendToTextList(attLogInfo+attname);
			}
				
			doc.replaceItemValue("ticketid", mdoc.getTicketid());
			//用户信息userinfo
			doc.replaceItemValue("createby", mdoc.getCreateby());
			doc.replaceItemValue("reportedbyid", mdoc.getReportedbyid());
			doc.replaceItemValue("reportedphone", mdoc.getReportedphone());
			doc.replaceItemValue("reportedemail", mdoc.getReportedemail());
			//外部信息extinfo
			doc.replaceItemValue("custremark", mdoc.getCustremark());
			doc.replaceItemValue("custusername", mdoc.getCustusername());
			doc.replaceItemValue("custcallnumber", mdoc.getCustcallnumber());
			doc.replaceItemValue("custemail", mdoc.getCustemail());
			//事件工单详细描述incidentinfo
			doc.replaceItemValue("description", mdoc.getDescription());
			doc.replaceItemValue("fromsiteid", mdoc.getFromsiteid());
			doc.replaceItemValue("internalpriority", mdoc.getInternalpriority());
			doc.replaceItemValue("descriptionlongdescription", mdoc.getDescriptionlongdescription());
			//关联信息associateinfo
			doc.replaceItemValue("assetdescription", mdoc.getAssetdescription());
			doc.replaceItemValue("locationdescription", mdoc.getLocationdescription());
			doc.replaceItemValue("siteid", mdoc.getSiteid());
			doc.replaceItemValue("orgid", mdoc.getOrgid());			
			//日期date
			doc.replaceItemValue("reportdate", mdoc.getReportdate());
			doc.replaceItemValue("affecteddate", mdoc.getAffecteddate());
			doc.replaceItemValue("targetcontactdate", mdoc.getTargetcontactdate());
			doc.replaceItemValue("targetstart", mdoc.getTargetstart());
			doc.replaceItemValue("targetfinish", mdoc.getTargetfinish());
			doc.replaceItemValue("actualcontactdate", mdoc.getActualcontactdate());
			doc.replaceItemValue("actualstart", mdoc.getActualstart());
			doc.replaceItemValue("actualfinish", mdoc.getActualfinish());
			
			doc.save();
			db.recycle();	//释放数据库
		}
		catch(NotesException e){
			backinfo = e.toString();
		}
		return backinfo;
	}
	
	public String getTime(){
        SimpleDateFormat formatter = new SimpleDateFormat("yy-MM-dd HH:mm:ss");
        Date now = new Date();
        String strTime = formatter.format(now);
        return strTime;
    }
}
