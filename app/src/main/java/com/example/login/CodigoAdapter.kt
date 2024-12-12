package com.example.login

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CodigoAdapter(
    private val codigos: List<String>,
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<CodigoAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val btnCodigo: TextView = itemView.findViewById(R.id.btnCodigo)
        val descripcionCodigo: TextView = itemView.findViewById(R.id.descripcionCodigo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_button, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val codigo = codigos[position]
        holder.btnCodigo.text = codigo
        holder.descripcionCodigo.text = "Tap para más detalles" // Descripción fija

        // Agregar acción al presionar
        holder.itemView.setOnClickListener { onItemClick(codigo) }
    }

    override fun getItemCount(): Int = codigos.size
}
