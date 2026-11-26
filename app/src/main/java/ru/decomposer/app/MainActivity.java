package ru.decomposer.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.*;

public final class MainActivity extends Activity {
    private static final int BACK = Color.rgb(8,10,14), CARD = Color.rgb(25,29,36), BLUE = Color.rgb(21,127,255), WHITE = Color.WHITE, GRAY = Color.rgb(148,155,167);
    private final ArrayList<Task> roots = new ArrayList<>();
    private LinearLayout rows;
    private Task active;
    @Override public void onBackPressed(){if(active!=null){active=null;render();}else super.onBackPressed();}
    private long nextId = 1;
    private static class Task {
        long id; String name; boolean done, expanded = true; final ArrayList<Task> children = new ArrayList<>();
        Task(long id, String name) { this.id=id; this.name=name; }
    }
    @Override public void onCreate(Bundle b) { super.onCreate(b); getWindow().setStatusBarColor(BACK); getWindow().setNavigationBarColor(BACK); load(); render(); }
    private int dp(float n) { return (int)(n*getResources().getDisplayMetrics().density+0.5f); }
    private GradientDrawable bg(int color, int radius) { GradientDrawable d=new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); return d; }
    private TextView text(String s, int size, int color) { TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setGravity(Gravity.CENTER_VERTICAL); return t; }
    private void render() {
        FrameLayout frame = new FrameLayout(this); frame.setBackgroundColor(BACK);
        LinearLayout page=new LinearLayout(this); page.setOrientation(LinearLayout.VERTICAL);
        LinearLayout header=new LinearLayout(this); header.setGravity(Gravity.CENTER_VERTICAL); header.setPadding(dp(20),dp(12),dp(16),dp(12));
        if(active!=null && !roots.contains(active))active=null;
        if(active!=null){TextView back=text("‹",32,WHITE);header.addView(back,new LinearLayout.LayoutParams(dp(40),dp(54)));back.setOnClickListener(v->{active=null;render();});}
        TextView title=text(active==null?"Декомпозитор":active.name,23,WHITE); title.setTypeface(null,Typeface.BOLD);header.addView(title,new LinearLayout.LayoutParams(0,dp(54),1));
        page.addView(header);
        ScrollView scroll=new ScrollView(this); scroll.setFillViewport(true);
        rows=new LinearLayout(this);rows.setPadding(dp(14),dp(8),dp(14),dp(84));rows.setOrientation(LinearLayout.VERTICAL);scroll.addView(rows);
        page.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));frame.addView(page);
        TextView fab=text("+",36,WHITE);fab.setGravity(Gravity.CENTER);fab.setBackground(bg(BLUE,50));FrameLayout.LayoutParams fp=new FrameLayout.LayoutParams(dp(60),dp(60),Gravity.BOTTOM|Gravity.RIGHT);fp.setMargins(0,0,dp(24),dp(24));frame.addView(fab,fp);fab.setOnClickListener(v->edit(active,null));fab.setContentDescription("Добавить задачу");
        if(android.os.Build.VERSION.SDK_INT>=35) frame.setOnApplyWindowInsetsListener((v,insets)->{
            if(android.os.Build.VERSION.SDK_INT>=30){
                android.graphics.Insets safe=insets.getInsets(android.view.WindowInsets.Type.systemBars() | android.view.WindowInsets.Type.displayCutout());
                v.setPadding(safe.left,safe.top,safe.right,safe.bottom);
            }
            return insets;
        });
        setContentView(frame);
        if(android.os.Build.VERSION.SDK_INT>=35) frame.requestApplyInsets();
        if(roots.isEmpty()){TextView hint=text("Нажмите +, чтобы создать первую задачу",16,GRAY);hint.setGravity(Gravity.CENTER);rows.addView(hint,new LinearLayout.LayoutParams(-1,dp(150)));}
        else if(active!=null)addRow(active,0);else for(Task task:roots)addProject(task);
    }
    private TextView progressRing(int progress) {
        TextView ring=text(progress+"%",progress==100?12:14,WHITE);
        ring.setTypeface(null,Typeface.BOLD);ring.setGravity(Gravity.CENTER);
        ring.setBackground(new android.graphics.drawable.Drawable(){
            final android.graphics.Paint paint=new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
            @Override public void draw(android.graphics.Canvas canvas){
                android.graphics.Rect b=getBounds();float w=dp(4), inset=w/2+dp(1);
                android.graphics.RectF oval=new android.graphics.RectF(b.left+inset,b.top+inset,b.right-inset,b.bottom-inset);
                paint.setStyle(android.graphics.Paint.Style.STROKE);paint.setStrokeWidth(w);paint.setColor(GRAY);
                canvas.drawOval(oval,paint);
                if(progress>0){paint.setColor(BLUE);canvas.drawArc(oval,-90,360f*progress/100f,false,paint);}
            }
            @Override public void setAlpha(int alpha){paint.setAlpha(alpha);}
            @Override public void setColorFilter(android.graphics.ColorFilter filter){paint.setColorFilter(filter);}
            @Override public int getOpacity(){return android.graphics.PixelFormat.TRANSLUCENT;}
        });
        return ring;
    }
    private void addProject(Task t){
        LinearLayout card=new LinearLayout(this);card.setGravity(Gravity.CENTER_VERTICAL);card.setPadding(dp(8),dp(7),dp(8),dp(7));card.setBackground(bg(CARD,18));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(67));lp.setMargins(0,dp(6),0,0);
        TextView ring=progressRing(percent(t));card.addView(ring,new LinearLayout.LayoutParams(dp(47),dp(47)));
        TextView name=text(t.name,16,WHITE);name.setMaxLines(2);LinearLayout.LayoutParams np=new LinearLayout.LayoutParams(0,-1,1);np.setMargins(dp(12),0,dp(4),0);card.addView(name,np);
        TextView arrow=text("›",28,BLUE);arrow.setGravity(Gravity.CENTER);card.addView(arrow,new LinearLayout.LayoutParams(dp(32),-1));
        card.setOnClickListener(v->{active=t;render();});card.setOnLongClickListener(v->{options(t);return true;});rows.addView(card,lp);
    }
    private void addRow(Task t,int depth) {
        LinearLayout card=new LinearLayout(this); card.setGravity(Gravity.CENTER_VERTICAL);card.setPadding(dp(8),dp(7),dp(8),dp(7));card.setBackground(bg(CARD,18));
        LinearLayout.LayoutParams cardLp=new LinearLayout.LayoutParams(-1,dp(67));cardLp.setMargins(dp(Math.min(depth,6)*17),dp(6),0,0);
        if(depth>0){TextView branch=text("└",25,GRAY);branch.setGravity(Gravity.CENTER);card.addView(branch,new LinearLayout.LayoutParams(dp(22),-1));}
        int progress=percent(t);
        TextView ring=progressRing(progress);card.addView(ring,new LinearLayout.LayoutParams(dp(47),dp(47)));
        TextView name=text(t.name,16,WHITE);name.setMaxLines(2);name.setEllipsize(android.text.TextUtils.TruncateAt.END); LinearLayout.LayoutParams np=new LinearLayout.LayoutParams(0,-1,1);np.setMargins(dp(12),0,dp(4),0);card.addView(name,np);
        if(!t.children.isEmpty()){TextView count=text(completed(t)+"/"+leaves(t),12,GRAY);card.addView(count);TextView arrow=text(t.expanded?"⌄":"›",25,WHITE);arrow.setGravity(Gravity.CENTER);card.addView(arrow,new LinearLayout.LayoutParams(dp(32),-1));arrow.setOnClickListener(v->{t.expanded=!t.expanded;save();render();});}
        else {CheckBox box=new CheckBox(this);box.setButtonTintList(android.content.res.ColorStateList.valueOf(BLUE));box.setChecked(t.done);card.addView(box);box.setOnCheckedChangeListener((button,checked)->{t.done=checked;save();render();});}
        rows.addView(card,cardLp);
        name.setOnClickListener(v->options(t));ring.setOnClickListener(v->options(t));card.setOnLongClickListener(v->{options(t);return true;});
        if(t.expanded)for(Task c:t.children)addRow(c,depth+1);
    }
    private int leaves(Task t){if(t.children.isEmpty())return 1;int n=0;for(Task c:t.children)n+=leaves(c);return n;}
    private int completed(Task t){if(t.children.isEmpty())return t.done?1:0;int n=0;for(Task c:t.children)n+=completed(c);return n;}
    private int percent(Task t){return (int)Math.round(100.0*completed(t)/leaves(t));}
    private void options(Task t){String[] choices={"Добавить подзадачу","Переименовать","Переместить","Удалить"};new AlertDialog.Builder(this).setTitle(t.name).setItems(choices,(dialog,which)->{if(which==0)edit(t,null);if(which==1)edit(null,t);if(which==2)move(t);if(which==3)delete(t);}).show();}
    private void edit(Task parent,Task editing){EditText input=new EditText(this);input.setSingleLine(false);input.setTextColor(WHITE);input.setHintTextColor(GRAY);input.setHint("Название задачи");if(editing!=null)input.setText(editing.name);
        LinearLayout wrap=new LinearLayout(this);wrap.setPadding(dp(20),0,dp(20),0);wrap.addView(input,new LinearLayout.LayoutParams(-1,-2));
        new AlertDialog.Builder(this).setTitle(editing!=null?"Переименовать":"Новая задача").setView(wrap).setNegativeButton("Отмена",null).setPositiveButton("Сохранить",(d,w)->{String s=input.getText().toString().trim();if(s.isEmpty())return;if(editing!=null)editing.name=s;else{Task n=new Task(nextId++,s);if(parent==null)roots.add(n);else{parent.children.add(n);parent.expanded=true;parent.done=false;}}save();render();}).show(); }
    private void delete(Task t){new AlertDialog.Builder(this).setTitle("Удалить задачу?").setMessage("Все вложенные подзадачи тоже будут удалены.").setNegativeButton("Отмена",null).setPositiveButton("Удалить",(d,w)->{remove(t,roots);save();render();}).show();}
    private boolean remove(Task t,List<Task> list){if(list.remove(t))return true;for(Task c:list)if(remove(t,c.children))return true;return false;}
    private boolean contains(Task a,Task b){if(a==b)return true;for(Task x:a.children)if(contains(x,b))return true;return false;}
    private void move(Task t){ArrayList<Task> possible=new ArrayList<>();ArrayList<String> names=new ArrayList<>();names.add("В корень дерева");collect(roots,possible,names,t,0);new AlertDialog.Builder(this).setTitle("Переместить в...").setItems(names.toArray(new String[0]),(d,i)->{Task parent=i==0?null:possible.get(i-1);if(parent==null && roots.contains(t))return;remove(t,roots);if(parent==null)roots.add(t);else{parent.children.add(t);parent.expanded=true;parent.done=false;}save();render();}).show();}
    private void collect(List<Task> list,List<Task> out,List<String> names,Task moving,int depth){for(Task t:list){if(contains(moving,t))continue;out.add(t);StringBuilder indent=new StringBuilder();for(int n=0;n<Math.min(8,depth);n++)indent.append("  ");names.add(indent.toString()+t.name);collect(t.children,out,names,moving,depth+1);}}
    private JSONObject toJson(Task t)throws Exception{JSONObject o=new JSONObject();o.put("id",t.id);o.put("name",t.name);o.put("done",t.done);o.put("expanded",t.expanded);JSONArray a=new JSONArray();for(Task c:t.children)a.put(toJson(c));o.put("children",a);return o;}
    private Task fromJson(JSONObject o)throws Exception{Task t=new Task(o.getLong("id"),o.getString("name"));t.done=o.optBoolean("done");t.expanded=o.optBoolean("expanded",true);nextId=Math.max(nextId,t.id+1);JSONArray a=o.optJSONArray("children");if(a!=null)for(int i=0;i<a.length();i++)t.children.add(fromJson(a.getJSONObject(i)));return t;}
    private void save(){try{JSONArray a=new JSONArray();for(Task t:roots)a.put(toJson(t));getPreferences(Context.MODE_PRIVATE).edit().putString("tasks",a.toString()).apply();}catch(Exception e){android.util.Log.e("Decomposer","Save failed",e);}}
    private void load(){try{String data=getPreferences(Context.MODE_PRIVATE).getString("tasks",null);if(data==null)return;JSONArray a=new JSONArray(data);for(int i=0;i<a.length();i++)roots.add(fromJson(a.getJSONObject(i)));}catch(Exception e){android.util.Log.e("Decomposer","Load failed",e);}}
}
