require 'elasticsearch'

posts = File.open("./posts.txt").each_line.to_a.map(&:chomp)
Jekyll::Hooks.register :posts, :post_render do |post|
  next unless posts.include? post.basename
  puts "Indexing #{post.url}..."
end
