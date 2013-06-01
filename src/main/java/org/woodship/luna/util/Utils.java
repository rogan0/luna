package org.woodship.luna.util;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.persistence.Entity;

import org.woodship.luna.db.HierarchialEntity;

import ru.xpoft.vaadin.SpringApplicationContext;

import com.vaadin.addon.jpacontainer.EntityProvider;
import com.vaadin.addon.jpacontainer.JPAContainer;
import com.vaadin.data.fieldgroup.BeanFieldGroup;
import com.vaadin.data.fieldgroup.Caption;
import com.vaadin.data.fieldgroup.FieldGroup;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.Table;

public class Utils {
	/**
	 * 为table中已经存在的列设置caption，(根据{@link Caption},beanClass的字段上有该注解则增加 )
	 * @param table
	 * @param beanClass
	 */
	public static void setTableCaption(Table table,Class<?> beanClass){
		Object[] vcols = table.getVisibleColumns();
		for(Object col : vcols){
			try {
				Field f = beanClass.getDeclaredField(col.toString());
				Caption caption = f.getAnnotation(Caption.class);
				if(caption != null){
					table.setColumnHeader(col, caption.value());
				}
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (NoSuchFieldException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 为table设置默认列与caption(根据{@link Caption},beanClass的字段上有该注解则增加 )
	 * @param table
	 * @param beanClass
	 */
	public static void setTableDefaultHead(Table table,Class<?> beanClass){
		Map<String, String> map = new LinkedHashMap<String, String>();
		//准备数据
		for(Field f : beanClass.getDeclaredFields()){
			Caption caption = f.getAnnotation(Caption.class);
			if(caption != null){
				map.put(f.getName(), caption.value());
			}
		}
		//设置数据
		Object[] cols = map.keySet().toArray();
		table.setVisibleColumns(cols);
		for(Object col : cols){
			table.setColumnHeader(col, map.get(col));
		}
	}

	/**
	 * 为指定{@link BeanFieldGroup}绑定默认字段。(根据{@link Caption}，beanClass的字段上有该注解则绑定 ) </p>
	 * 该方法只负责确定生成哪些字段，生成Caption，，创建字段
	 * @param fieldGroup
	 * @param beanClass 要绑定到{@link BeanFieldGroup}上的beanClass
	 * @return 
	 */
	public static void buildAndBindFieldGroup(FieldGroup fieldGroup, Class<?> beanClass,ComponentContainer layout){
		for(Field f : beanClass.getDeclaredFields()){
			Caption caption = f.getAnnotation(Caption.class);
			if(caption != null){
				Class<?> type = f.getType();
				Entity e = type.getAnnotation(Entity.class);
				if (e != null || Enum.class.isAssignableFrom(type)){//实体、enum类型自动生成下拉框
					layout.addComponent(fieldGroup.buildAndBind(caption.value(),f.getName(),ComboBox.class));
				}
				else{
					layout.addComponent(fieldGroup.buildAndBind(caption.value(),f.getName()));
				}
			}
		}
	}

	/**
	 * 根据类获得增加功能按钮的ID
	 * @param calss
	 * @return
	 */
	public static String getAddActionId(Class<?> calss){
		return calss.getName()+":ADD";
	}
	/**
	 * 根据类获得删除功能按钮的ID
	 * @param calss
	 * @return
	 */
	public static String getDelActionId(Class<?> calss){
		return calss.getName()+":DEL";
	}
	/**
	 * 根据类获得编辑功能按钮的ID
	 * @param calss
	 * @return
	 */
	public static String getEditActionId(Class<?> calss){
		return calss.getName()+":EDIT";
	}

	/**
	 * 为指定实体类生成JPAContaine
	 * @param entityClass
	 * @return
	 */
	public static <T> JPAContainer<T> getJPAContainer(final Class<T> entityClass){
		EntityProvider<T> ep =  getEntityProvider( entityClass);
		JPAContainer<T> container= new JPAContainer<T>(entityClass);
		container.setEntityProvider(ep);
		return container;
	}

	
	/**
	 * 为指定实体生成支持树的JPAContaine
	 * @param entityClass
	 * @return
	 */
	public static <T extends HierarchialEntity<T>> JPAContainer<T> getHierarchialJPAContainer(final Class<T> entityClass){
		EntityProvider<T> ep =  getEntityProvider( entityClass);

		class HJPAContainer<H extends HierarchialEntity<H>> extends JPAContainer<H>{
			private static final long serialVersionUID = 1L;

			public HJPAContainer(Class<H> entityClass, EntityProvider<H> entityProvide) {
				super(entityClass);
				setEntityProvider(entityProvide);
				setParentProperty("parent");
			}
			@Override
			public boolean areChildrenAllowed(Object itemId) {
				return super.areChildrenAllowed(itemId)
						&& !getItem(itemId).getEntity().isLeaf();
			}
		}
		
		JPAContainer<T> container= new HJPAContainer<T>(entityClass, ep);
		container.setEntityProvider(ep);
		return container;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static  <T> EntityProvider<T> getEntityProvider(Class<T> entityClass){
		String beanName = entityClass.getSimpleName()+"EntityProvider";
		Map<String, EntityProvider> beans = SpringApplicationContext.getApplicationContext().getBeansOfType(EntityProvider.class);
		EntityProvider<T> ep = null;
		for(Entry<String, EntityProvider> en : beans.entrySet()) {
			if(beanName.equalsIgnoreCase(en.getKey())){
				ep = en.getValue();
			}
		}
		if(ep == null){
			throw new RuntimeException("未找到类 "+entityClass.getName()+" 对应的EntityProvider bean");
		}
		return ep;
	}


}
