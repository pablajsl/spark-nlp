require 'elasticsearch'

posts = File.open("./changes.txt") \
  .each_line \
  .to_a \
  .map(&:chomp) \
  .select { |v| v.start_with?("docs/_posts") } \
  .map { |v| File.basename(v) }
Jekyll::Hooks.register :posts, :post_render do |post|
  next unless posts.include? post.basename
  puts "Indexing #{post.url}..."
end
