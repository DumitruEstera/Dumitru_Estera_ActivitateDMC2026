package com.example.proiect.adapters;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import com.example.proiect.R;
import com.example.proiect.models.Person;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PersonAdapter extends BaseAdapter implements Filterable {

    private final Context context;
    private final LayoutInflater inflater;

    private List<Person> source;
    private List<Person> filtered;
    private String departmentFilter = "";

    public PersonAdapter(Context context, List<Person> persons) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.source = new ArrayList<>(persons);
        this.filtered = new ArrayList<>(persons);
    }

    public void setPersons(List<Person> persons) {
        this.source = new ArrayList<>(persons);
        this.filtered = new ArrayList<>(persons);
        notifyDataSetChanged();
    }

    public void setDepartmentFilter(String department) {
        this.departmentFilter = department == null ? "" : department;
        getFilter().filter("");
    }

    @Override
    public int getCount() {
        return filtered == null ? 0 : filtered.size();
    }

    @Override
    public Person getItem(int position) {
        return filtered.get(position);
    }

    @Override
    public long getItemId(int position) {
        return filtered.get(position).getId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder h;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_person, parent, false);
            h = new ViewHolder();
            h.name = convertView.findViewById(R.id.item_person_name);
            h.dept = convertView.findViewById(R.id.item_person_dept);
            h.employee = convertView.findViewById(R.id.item_person_employee);
            h.zones = convertView.findViewById(R.id.item_person_zones);
            h.faces = convertView.findViewById(R.id.item_person_faces);
            convertView.setTag(h);
        } else {
            h = (ViewHolder) convertView.getTag();
        }

        Person p = filtered.get(position);
        h.name.setText(TextUtils.isEmpty(p.getName())
                ? context.getString(R.string.persons_unknown_name) : p.getName());
        h.dept.setText(TextUtils.isEmpty(p.getDepartment())
                ? "—" : p.getDepartment());
        h.employee.setText(context.getString(R.string.persons_employee_fmt,
                TextUtils.isEmpty(p.getEmployeeId()) ? "—" : p.getEmployeeId()));

        List<String> zones = p.getAuthorizedZones();
        if (zones == null || zones.isEmpty()) {
            h.zones.setVisibility(View.GONE);
        } else {
            h.zones.setVisibility(View.VISIBLE);
            h.zones.setText(TextUtils.join(" • ", zones));
        }

        h.faces.setText(context.getResources().getQuantityString(
                R.plurals.persons_faces_count, p.getFaceCount(), p.getFaceCount()));

        return convertView;
    }

    @Override
    public Filter getFilter() {
        return personFilter;
    }

    private final Filter personFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            String query = constraint == null ? "" : constraint.toString().trim().toLowerCase(Locale.US);
            String dept = departmentFilter == null ? "" : departmentFilter.trim();

            List<Person> result = new ArrayList<>();
            if (source != null) {
                for (Person p : source) {
                    if (!query.isEmpty()) {
                        String name = p.getName() == null ? "" : p.getName().toLowerCase(Locale.US);
                        String emp = p.getEmployeeId() == null ? "" : p.getEmployeeId().toLowerCase(Locale.US);
                        if (!name.contains(query) && !emp.contains(query)) continue;
                    }
                    if (!dept.isEmpty()) {
                        String pDept = p.getDepartment() == null ? "" : p.getDepartment();
                        if (!pDept.equalsIgnoreCase(dept)) continue;
                    }
                    result.add(p);
                }
            }
            FilterResults fr = new FilterResults();
            fr.values = result;
            fr.count = result.size();
            return fr;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            filtered = results.values == null ? new ArrayList<>() : (List<Person>) results.values;
            notifyDataSetChanged();
        }
    };

    private static class ViewHolder {
        TextView name;
        TextView dept;
        TextView employee;
        TextView zones;
        TextView faces;
    }
}
