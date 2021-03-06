package moe.nikky.curseproxy.dao

import io.ktor.html.insert
import moe.nikky.curseproxy.LOG
import moe.nikky.curseproxy.dao.Addons.categoryList
import moe.nikky.curseproxy.dao.Addons.dateCreated
import moe.nikky.curseproxy.dao.Addons.dateModified
import moe.nikky.curseproxy.dao.Addons.dateReleased
import moe.nikky.curseproxy.dao.Addons.id
import moe.nikky.curseproxy.dao.Addons.name
import moe.nikky.curseproxy.dao.Addons.primaryAuthorName
import moe.nikky.curseproxy.dao.Addons.primaryCategoryName
import moe.nikky.curseproxy.dao.Addons.sectionName
import moe.nikky.curseproxy.model.Section
import moe.nikky.curseproxy.model.graphql.Addon
import org.jetbrains.squash.connection.DatabaseConnection
import org.jetbrains.squash.connection.transaction
import org.jetbrains.squash.dialects.h2.H2Connection
import org.jetbrains.squash.expressions.eq
import org.jetbrains.squash.expressions.like
import org.jetbrains.squash.expressions.literal
import org.jetbrains.squash.expressions.or
import org.jetbrains.squash.query.from
import org.jetbrains.squash.query.limit
import org.jetbrains.squash.query.select
import org.jetbrains.squash.query.where
import org.jetbrains.squash.results.ResultRow
import org.jetbrains.squash.results.get
import org.jetbrains.squash.schema.create
import org.jetbrains.squash.statements.deleteFrom
import org.jetbrains.squash.statements.fetch
import org.jetbrains.squash.statements.insertInto
import org.jetbrains.squash.statements.values

fun ResultRow.toSparseAddon() = Addon(
        id = this[Addons.id],
        name = this[Addons.name],
        primaryAuthorName = this[Addons.primaryAuthorName],
        primaryCategoryName = this[Addons.primaryCategoryName],
        sectionName = Section.valueOf(this[Addons.sectionName]),
        dateModified = this[Addons.dateModified],
        dateCreated = this[Addons.dateCreated],
        dateReleased = this[Addons.dateReleased],
        categoryList = this[Addons.categoryList]
)

//TODO: add more database row conversions


class AddonDatabase(val db: DatabaseConnection = H2Connection.createMemoryConnection()) : AddonStorage {
    init {
        db.transaction {
            databaseSchema().create(Addons)
        }
    }

    override fun getAddon(id: Int) = db.transaction {
        val row = from(Addons).where { Addons.id eq id }.execute().singleOrNull()
        row?.toSparseAddon()
    }

    override fun getAll(size: Long?, name: String?, author: String?, category: String?, section: Section?) = db.transaction {
        from(Addons)
                .select()
                .apply {
                    name?.let {
                        LOG.debug("added name filter '$it'")
                        where { Addons.name like it }
                    }
                    author?.let {
                        LOG.debug("added author filter '$it'")
                        where { Addons.primaryAuthorName like it }
                    }
                    category?.let {
                        LOG.debug("added category filter '$it'")
                        where { (Addons.primaryCategoryName like it) or (Addons.categoryList like "%$category%") }
                    }
                    section?.let {
                        LOG.debug("added section filter '$it'")
                        where { Addons.sectionName eq it.toString() }
                    }
                    size?.let {
                        LOG.debug("added size limit '$it'")
                        limit(size)
                    }
                }
//                .orderBy(Addons.date, ascending = false)
                .execute()
                .map { it.toSparseAddon() }
                .toList()
    }

    override fun createAddon(addon: Addon) {

        db.transaction {
            deleteFrom(Addons)
                    .where(Addons.id eq addon.id)
                    .execute()

            insertInto(Addons).values {
                it[id] = addon.id
                it[name] = addon.name
                it[primaryAuthorName] = addon.primaryAuthorName
                it[primaryCategoryName] = addon.primaryCategoryName
                it[sectionName] = addon.sectionName.toString()
                it[dateModified] = addon.dateModified
                it[dateCreated] = addon.dateCreated
                it[dateReleased] = addon.dateReleased
                it[categoryList] = addon.categoryList
            }.execute()
        }
    }

    override fun close() {}

}